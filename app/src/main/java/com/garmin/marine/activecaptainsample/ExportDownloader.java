/*------------------------------------------------------------------------------
Copyright 2021 Garmin Ltd. or its subsidiaries.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
------------------------------------------------------------------------------*/

package com.garmin.marine.activecaptainsample;

import android.util.Log;

import com.garmin.marine.activecaptaincommunitysdk.ActiveCaptainDatabase;
import com.garmin.marine.activecaptainsample.contract.response.ExportResponse;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.security.MessageDigest;
import java.util.List;
import java.util.zip.GZIPInputStream;

public class ExportDownloader {
    private final String basePath;
    private final ActiveCaptainDatabase database;

    public ExportDownloader(ActiveCaptainDatabase database, String basePath) {
        if (database == null) {
            throw new IllegalArgumentException("database must not be null.");
        }

        if (basePath == null) {
            throw new IllegalArgumentException("basePath must not be null.");
        }

        this.database = database;
        this.basePath = basePath;
    }

    public final void download(List<ExportResponse> exports) {
        try {
            for (ExportResponse export : exports) {
                Log.d("ExportDownloader", "Downloading: " + export.gzip.url);
                URL url = new URL(export.gzip.url);
                URLConnection connection = url.openConnection();
                connection.connect();

                File outputFile = new File(basePath, "active_captain_" + export.tileX + "_" + export.tileY + ".db");
                File gzipFile = new File(basePath, "active_captain_" + export.tileX + "_" + export.tileY + ".db.gz");

                InputStream input = new BufferedInputStream(url.openStream(), 8192);

                OutputStream output = new FileOutputStream(gzipFile);

                int count;
                byte[] data = new byte[1024];
                long total = 0;
                MessageDigest digest = MessageDigest.getInstance("MD5");

                // Download compressed data.
                while ((count = input.read(data)) > 0) {
                    total += count;

                    // Update MD5 hash of the compressed data.
                    digest.update(data, 0, count);

                    // Write the GZip file.
                    output.write(data, 0, count);
                }

                output.flush();

                output.close();
                input.close();

                // Confirm the entire file was downloaded.
                if (total != export.gzip.fileSize) {
                    throw new Exception("File size mismatch: " + export.tileX + " " + export.tileY + ", " + "Expected: " + export.gzip.fileSize + ", Actual: " + total);
                }

                // Confirm MD5 hash of file contents matches expected value.
                byte[] md5Bytes = digest.digest();
                StringBuilder md5 = new StringBuilder();
                for (byte b : md5Bytes) {
                    md5.append(Integer.toString((b & 0xff) + 0x100, 16).substring(1));
                }

                if (!md5.toString().equals(export.gzip.md5Hash)) {
                    throw new Exception("MD5 hash mismatch: " + export.tileX + " " + export.tileY + ", " + "Expected: " + export.gzip.md5Hash + ", Actual: " + md5);
                }

                decompressFile(gzipFile, outputFile);
                gzipFile.delete();

                Log.d("ExportDownloader", "Installing: " + export.tileX + " " + export.tileY + " " + outputFile.getPath());
                database.installTile(outputFile.getPath(), export.tileX, export.tileY);
            }
        } catch (Exception e) {
            Log.e("Error: ", e.getMessage());
        }
    }

    private void decompressFile(File compressedFile, File decompressedFile) throws IOException {
        InputStream input = new FileInputStream(compressedFile);
        OutputStream output = new FileOutputStream(decompressedFile);
        GZIPInputStream gzipInputStream = new GZIPInputStream(input);

        int count;
        byte[] data = new byte[1024];

        while ((count = gzipInputStream.read(data)) > 0) {
            output.write(data, 0, count);
        }

        output.flush();
        output.close();
        input.close();
    }
}