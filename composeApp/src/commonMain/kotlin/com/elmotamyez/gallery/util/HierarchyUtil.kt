package com.elmotamyez.gallery.util

import com.elmotamyez.gallery.data.model.Brand
import com.elmotamyez.gallery.data.model.Category
import com.elmotamyez.gallery.data.model.Product

/**
 * Returns a readable hierarchy path for a product, e.g.:
 *   "أدوات مكتبية › أقلام"
 *   "اكسسوارات ستانليس ستيل › أثواب › بنات"
 */
fun buildProductPath(
    product: Product,
    categories: List<Category>,
    brands: List<Brand>
): String {
    val catName   = categories.find { it.id == product.categoryId }?.name ?: ""
    val brand     = brands.find { it.id == product.brandId } ?: return catName
    val brandName = brand.name

    return if (brand.parentId != null) {
        val parentName = brands.find { it.id == brand.parentId }?.name ?: ""
        "$catName › $parentName › $brandName"
    } else {
        "$catName › $brandName"
    }
}

/**
 * Returns a readable path for a brand (sub or sub-sub), e.g.:
 *   "أدوات مكتبية › أقلام"
 *   "اكسسوارات ستانليس ستيل › أثواب › بنات"
 */
fun buildBrandPath(
    brand: Brand,
    categories: List<Category>,
    brands: List<Brand>
): String {
    val catName = categories.find { it.id == brand.categoryId }?.name ?: ""
    return if (brand.parentId != null) {
        val parentName = brands.find { it.id == brand.parentId }?.name ?: ""
        "$catName › $parentName › ${brand.name}"
    } else {
        "$catName › ${brand.name}"
    }
}
