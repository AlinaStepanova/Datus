package com.example.datus;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.apache.tika.config.TikaConfig;
import org.apache.tika.exception.TikaException;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.language.LanguageIdentifier;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MediaType;
import org.apache.tika.mime.MediaTypeRegistry;
import org.apache.tika.mime.MimeType;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;


public class StorageDemoActivity extends AppCompatActivity {

    private static EditText textView;

    private static final int CREATE_REQUEST_CODE = 40;
    private static final int OPEN_REQUEST_CODE = 41;
    private static final int SAVE_REQUEST_CODE = 42;
    private long size = 0;
    private final Metadata metadata = new Metadata();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_storage_demo);

        textView = (EditText) findViewById(R.id.fileText);
    }

    public void newFile(View view) {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);

        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TITLE, "newfile.txt");

        startActivityForResult(intent, CREATE_REQUEST_CODE);
    }

    public void onActivityResult(int requestCode, int resultCode,
                                 Intent resultData) {

        super.onActivityResult(requestCode, resultCode, resultData);
        Uri currentUri = null;
        String content = "";
        String alias = "";
        MediaTypeRegistry registry = MediaTypeRegistry.getDefaultRegistry();
//        listAllTypes();
        if (resultCode == Activity.RESULT_OK) {

            if (requestCode == CREATE_REQUEST_CODE) {
                if (resultData != null) {
                    textView.setText("");
                }
            } else if (requestCode == SAVE_REQUEST_CODE) {

                if (resultData != null) {
                    currentUri = resultData.getData();
                    writeFileContent(currentUri);
                }
            } else if (requestCode == OPEN_REQUEST_CODE) {

                if (resultData != null) {
                    currentUri = resultData.getData();

                    try {
                        String language = "not a plain text or not identified";
                        String type;
                        String extention = "";
                        content =
                                readFileContent(currentUri);
                        try {
                            MediaType mimetype = getMediaType(currentUri);
                            Set<MediaType> aliases = registry.getAliases(mimetype);
                            alias = mimetype + ", also known as " + aliases;
                            if (mimetype.getType().equals("text") && mimetype.getSubtype().equals("plain")) {
                                language = detectLang(content);
                            }
                            extention = detectExtension(mimetype);
                            type = mimetype.getType();
                            content = "Type: " + type + "\nLanguage: " + language + "\nExtension: " + extention;
                        } catch (TikaException e) {
                            e.printStackTrace();
                        }
                        textView.setText(content);
                    } catch (IOException e) {
                        // Handle error here
                    }
                }
            }
        }

        Toast.makeText(getBaseContext(), alias,
                Toast.LENGTH_LONG).show();
    }

    // limit content parsing to some extent not to be so heavy
    private String readFileContent(Uri uri) throws IOException {

        InputStream inputStream =
                getContentResolver().openInputStream(uri);
        BufferedReader reader =
                new BufferedReader(new InputStreamReader(
                        inputStream));
        StringBuilder stringBuilder = new StringBuilder();
        String currentline;
        while ((currentline = reader.readLine()) != null) {
            stringBuilder.append(currentline + "\n");
        }
        inputStream.close();
        return stringBuilder.toString();
    }

    private void writeFileContent(Uri uri) {
        try {
            ParcelFileDescriptor pfd =
                    this.getContentResolver().
                            openFileDescriptor(uri, "w");

            FileOutputStream fileOutputStream =
                    new FileOutputStream(pfd.getFileDescriptor());

            String textContent =
                    textView.getText().toString();

            fileOutputStream.write(textContent.getBytes());

            fileOutputStream.close();
            pfd.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void openFile(View view) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/plain");
        startActivityForResult(intent, OPEN_REQUEST_CODE);
    }

    public void saveFile(View view) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/plain");

        startActivityForResult(intent, SAVE_REQUEST_CODE);
    }

    public MediaType getMediaType(Uri uri) throws TikaException, IOException {
        InputStream inputStream =
                getContentResolver().openInputStream(uri);
        TikaConfig tika = new TikaConfig();

        MediaType mimetype = tika.getDetector().detect(
                TikaInputStream.get(inputStream), new Metadata());
        String value = metadata.get(Metadata.CONTENT_LENGTH);

        if (null != value && !value.isEmpty()) {
            size = Long.valueOf(value);
        } else {
            try (final TikaInputStream tis = TikaInputStream.get(inputStream)) {
                size = tis.getLength();
            }

            metadata.set(Metadata.CONTENT_LENGTH, Long.toString(size));
        }

        System.out.println(size);
        return mimetype;
//        return "type " + mimetype.getType() + " subtype " + mimetype.getSubtype();
    }

    public String detectLang(String content) {
        LanguageIdentifier identifier = new LanguageIdentifier(content);
        String language = identifier.getLanguage();
        return language;
    }

    public String detectExtension(MediaType mediatype) {
        TikaConfig tika = null;
        String extension = "";
        AtomicReference<String> mimeTypeRef = new AtomicReference<>(null);
        mimeTypeRef.set(mediatype.toString());

        String mimeType = mimeTypeRef.get();
        try {
            MimeType mimetype;
            tika = new TikaConfig();
            mimetype = tika.getMimeRepository().forName(mimeType.toString());
            extension = mimetype.getExtension();

            if (mimeType != null && mimeType.equals("application/gzip") && extension.equals(".tgz")) {
                extension = ".gz";
            }

        } catch (TikaException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return extension;

    }

    public static void listAllTypes() {
        MediaTypeRegistry registry = MediaTypeRegistry.getDefaultRegistry();

        for (MediaType type : registry.getTypes()) {
            Set<MediaType> aliases = registry.getAliases(type);
            System.out.println(type + ", also known as " + aliases);
        }
    }


}


