package com.rejowan.pdfreaderpro.util

import com.itextpdf.kernel.utils.DefaultSafeXmlParserFactory
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.parsers.SAXParserFactory

/**
 * Android root-cause fix for iText XMP parsing failures.
 *
 * Android's [DocumentBuilderFactory.setXIncludeAware] always throws
 * `UnsupportedOperationException: This parser does not support specification
 * "Unknown" version "0.0"`. iText's [DefaultSafeXmlParserFactory] calls that
 * when reading PDF XMP metadata, so open/save of many PDFs (often large ones
 * with rich metadata) fails in every iText tool.
 *
 * Softens only the unsupported XInclude call; XXE-hardening features from the
 * superclass still apply before that call throws.
 */
class AndroidSafeXmlParserFactory : DefaultSafeXmlParserFactory() {

    override fun configureSafeDocumentBuilderFactory(factory: DocumentBuilderFactory) {
        try {
            super.configureSafeDocumentBuilderFactory(factory)
        } catch (_: UnsupportedOperationException) {
            // setXIncludeAware threw; remaining secure feature already applied.
            try {
                factory.setExpandEntityReferences(false)
            } catch (_: UnsupportedOperationException) {
                // Not available on this runtime.
            }
        }
    }

    override fun configureSafeSAXParserFactory(factory: SAXParserFactory) {
        try {
            super.configureSafeSAXParserFactory(factory)
        } catch (_: UnsupportedOperationException) {
            // Features already applied before setXIncludeAware threw.
        }
    }
}
