package com.elmotamyez.gallery.util

import com.elmotamyez.gallery.data.model.Receipt

actual fun exportReceiptToPdf(receipt: Receipt, fileName: String) {
    triggerBrowserPrint()
}

// js() must be in a function with no non-primitive parameters
private fun triggerBrowserPrint() {
    js("window.print()")
}
