package com.smalik.nicepdf;

import com.itextpdf.kernel.pdf.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public class JustInfo {

    private static Logger logger = LoggerFactory.getLogger(JustInfo.class);

    public static void main(String[] args) {

        JustInfo info = new JustInfo();
        try {
            info.getInfo(new File(args[0]));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void getInfo(File file) throws Exception {

        PdfDocument pdfDoc = new PdfDocument(new PdfReader(file));
        for (int i = 0; i < pdfDoc.getNumberOfPages(); i++) {

            PdfDictionary page = pdfDoc.getPage(i + 1).getPdfObject();
            PdfDictionary resources = page.getAsDictionary(PdfName.Resources);
            printDictionarytInfo(i+1, "", resources.getAsDictionary(PdfName.XObject));
        }
    }

    public void printDictionarytInfo(int page, String parentKey, PdfDictionary dictionary) {
        if (dictionary != null) {

            for (PdfName key : dictionary.keySet()) {

                PdfObject object = dictionary.get(key);
                if (object.isDictionary()) {
                    printDictionarytInfo(page, parentKey + key, dictionary.getAsDictionary(key));
                } else if (object.isArray()) {
                    printArrayInfo(page, parentKey + key, dictionary.getAsArray(key));
                } else {
                    logger.info("Page:" + page + ", Key:" + parentKey + key + ", Value:" + object);
                }
            }
        }
    }

    public void printArrayInfo(int page, String parentKey, PdfArray array) {

        for (int i = 0; i < array.size(); i++) {

            PdfObject object = array.get(i);
            if (object.isDictionary()) {
                printDictionarytInfo(page, parentKey+"["+i+"]", array.getAsDictionary(i));
            } else if (object.isArray()) {
                printArrayInfo(page, parentKey+"["+i+"]", array.getAsArray(i));
            } else {
                logger.info("Page:" + page + ", Key:" + parentKey+"["+i+"]" + ", ArrayValue:" + object);
            }
        }
    }

}
