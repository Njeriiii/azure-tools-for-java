/*
 * Copyright (c) Microsoft Corporation
 *
 * All rights reserved.
 *
 * MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.microsoft.azuretools.core.mvp.model.webapp;

import com.microsoft.azure.management.appservice.CsmPublishingProfileOptions;
import com.microsoft.azure.management.appservice.PublishingProfileFormat;
import com.microsoft.azure.management.appservice.WebAppBase;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import lombok.extern.java.Log;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.nio.file.Paths;

@Log
public class AppServiceUtils {
    @AzureOperation(
        value = "get publishing profile of function app[%s] with secret",
        params = {"$webAppBase.name()"},
        type = AzureOperation.Type.TASK
    )
    public static boolean getPublishingProfileXmlWithSecrets(WebAppBase webAppBase, String filePath) {
        final File file = new File(Paths.get(filePath, String.format("%s_%s.PublishSettings",
                                                                     webAppBase.name(), System.currentTimeMillis()))
                                        .toString());
        try {
            file.createNewFile();
        } catch (final IOException e) {
            log.warning("failed to create publishing profile xml file");
            return false;
        }
        try (final InputStream inputStream = webAppBase.manager().inner().webApps()
                                                       .listPublishingProfileXmlWithSecrets(webAppBase.resourceGroupName(),
                                                                                      webAppBase.name(),
                                                                                      new CsmPublishingProfileOptions().withFormat(
                                                                                          PublishingProfileFormat.FTP));
             final OutputStream outputStream = new FileOutputStream(file)
        ) {
            IOUtils.copy(inputStream, outputStream);
            return true;
        } catch (final IOException e) {
            e.printStackTrace();
            return false;
        }
    }
}
