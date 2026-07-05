package com.elmotamyez.gallery.util

import com.elmotamyez.gallery.data.model.Receipt

expect fun exportReceiptToPdf(receipt: Receipt, fileName: String = "receipt.pdf")
