package com.elmotamyez.gallery.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import com.elmotamyez.gallery.data.model.Product
import com.elmotamyez.gallery.util.formatPrice

// ── Stock state helpers ───────────────────────────────────────────────────────

private enum class StockState { OUT, LOW, OK }

private fun stockState(stock: Int) = when {
    stock == 0   -> StockState.OUT
    stock <= 5   -> StockState.LOW
    else         -> StockState.OK
}

private val StockState.color get() = when (this) {
    StockState.OUT -> Color(0xFFD32F2F)   // red
    StockState.LOW -> Color(0xFFF57C00)   // orange
    StockState.OK  -> Color(0xFF388E3C)   // green
}

private val StockState.label get() = when (this) {
    StockState.OUT -> "نفذ"
    StockState.LOW -> "قليل"
    StockState.OK  -> "متوفر"
}

// ── ProductImageSlider ────────────────────────────────────────────────────────

@Composable
fun ProductImageSlider(
    images: List<String>,
    modifier: Modifier = Modifier,
    onImageClick: ((pageIndex: Int) -> Unit)? = null
) {
    Box(modifier = modifier) {
        when {
            images.isEmpty() -> Box(
                Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Image, null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
                    modifier = Modifier.size(36.dp))
            }

            images.size == 1 -> Box(Modifier.fillMaxSize()) {
                AsyncImage(
                    model = images[0], contentDescription = null,
                    modifier = Modifier.fillMaxSize().let {
                        if (onImageClick != null) it.clickable { onImageClick(0) } else it
                    },
                    contentScale = ContentScale.Crop
                )
                if (onImageClick != null) ZoomHintIcon(Modifier.align(Alignment.TopEnd))
            }

            else -> {
                val pagerState = rememberPagerState(pageCount = { images.size })
                val scope = androidx.compose.runtime.rememberCoroutineScope()
                HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                    Box(Modifier.fillMaxSize()) {
                        AsyncImage(
                            model = images[page], contentDescription = null,
                            modifier = Modifier.fillMaxSize().let {
                                if (onImageClick != null) it.clickable { onImageClick(page) } else it
                            },
                            contentScale = ContentScale.Crop
                        )
                        if (onImageClick != null) ZoomHintIcon(Modifier.align(Alignment.TopEnd))
                    }
                }
                // Prev arrow
                if (pagerState.currentPage > 0) {
                    Box(
                        Modifier
                            .align(Alignment.CenterStart)
                            .padding(start = 4.dp)
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.35f))
                            .clickable { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) } },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                }
                // Next arrow
                if (pagerState.currentPage < images.size - 1) {
                    Box(
                        Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 4.dp)
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.35f))
                            .clickable { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) } },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                }
                // Dot indicators (clickable)
                Row(
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 5.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(images.size) { i ->
                        val selected = pagerState.currentPage == i
                        Box(
                            Modifier
                                .size(if (selected) 8.dp else 5.dp)
                                .clip(CircleShape)
                                .background(if (selected) Color.White else Color.White.copy(alpha = 0.5f))
                                .clickable { scope.launch { pagerState.animateScrollToPage(i) } }
                        )
                    }
                }
            }
        }
    }
}

// ── Zoom hint overlay ─────────────────────────────────────────────────────────

@Composable
private fun ZoomHintIcon(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .padding(4.dp)
            .size(20.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.45f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Default.Add, null, tint = Color.White, modifier = Modifier.size(12.dp))
    }
}

// ── Image lightbox dialog ─────────────────────────────────────────────────────

@Composable
fun ImageLightboxDialog(
    images: List<String>,
    startIndex: Int,
    onDismiss: () -> Unit
) {
    if (images.isEmpty()) return
    var currentIndex by remember { mutableStateOf(startIndex.coerceIn(0, images.lastIndex)) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.92f)),
            contentAlignment = Alignment.Center
        ) {
            // Full image
            AsyncImage(
                model = images[currentIndex],
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.78f)
            )

            // Close button — top end
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.18f))
                    .clickable { onDismiss() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.size(20.dp))
            }

            // Prev arrow (start side)
            if (currentIndex > 0) {
                Box(
                    Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 12.dp)
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.18f))
                        .clickable { currentIndex-- },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White, modifier = Modifier.size(24.dp))
                }
            }

            // Next arrow (end side)
            if (currentIndex < images.lastIndex) {
                Box(
                    Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 12.dp)
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.18f))
                        .clickable { currentIndex++ },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = Color.White, modifier = Modifier.size(24.dp))
                }
            }

            // Page counter
            if (images.size > 1) {
                Box(
                    Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 20.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.Black.copy(alpha = 0.5f))
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        "${currentIndex + 1} / ${images.size}",
                        color = Color.White,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// ── ProductCard ───────────────────────────────────────────────────────────────

@Composable
fun ProductCard(
    product: Product,
    isInCart: Boolean,
    quantity: Int,
    onAddToCart: () -> Unit,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit,
    modifier: Modifier = Modifier,
    categoryPath: String = ""   // e.g. "أدوات مكتبية › أقلام" or "cat › parent › sub"
) {
    val stockSt = stockState(product.stock)
    var lightboxIndex by remember { mutableStateOf(-1) }

    if (lightboxIndex >= 0 && product.displayImages.isNotEmpty()) {
        ImageLightboxDialog(
            images = product.displayImages,
            startIndex = lightboxIndex,
            onDismiss = { lightboxIndex = -1 }
        )
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 3.dp,
                shape = RoundedCornerShape(12.dp),
                ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.07f),
                spotColor   = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
            ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {

            // ── Product image ─────────────────────────────────────────────────
            ProductImageSlider(
                images = product.displayImages,
                onImageClick = { idx -> lightboxIndex = idx },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
            )

            // ── Name + price + stock ──────────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp, start = 10.dp, end = 10.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 17.sp,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = product.price.formatPrice(),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 0.sp
                )
                // ── Stock badge ───────────────────────────────────────────────
                StockBadge(stock = product.stock)
                // ── Category path ─────────────────────────────────────────────
                if (categoryPath.isNotBlank()) {
                    Text(
                        text = categoryPath,
                        fontSize = 9.sp,
                        lineHeight = 11.sp,
                        color = MaterialTheme.colorScheme.outline,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // ── Cart control row ──────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(all = 10.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                AnimatedContent(
                    targetState = isInCart,
                    transitionSpec = {
                        (fadeIn() + scaleIn(initialScale = 0.8f)) togetherWith
                        (fadeOut() + scaleOut(targetScale = 0.8f))
                    },
                    label = "cart_toggle"
                ) { inCart ->
                    if (!inCart) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(
                                    if (stockSt == StockState.OUT)
                                        MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                    else MaterialTheme.colorScheme.primary
                                )
                                .clickable(enabled = stockSt != StockState.OUT) { onAddToCart() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add to cart",
                                tint = Color.White, modifier = Modifier.size(22.dp))
                        }
                    } else {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary,
                            shadowElevation = 2.dp,
                            modifier = Modifier.height(32.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 6.dp)
                            ) {
                                Box(modifier = Modifier.size(24.dp).clickable { onDecrease() },
                                    contentAlignment = Alignment.Center) {
                                    Text("−", color = Color.White, fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold, lineHeight = 18.sp)
                                }
                                Text("$quantity", color = Color.White, fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp, modifier = Modifier.widthIn(min = 20.dp),
                                    textAlign = TextAlign.Center)
                                val atStockLimit = quantity >= product.stock
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clickable(enabled = !atStockLimit) { onIncrease() },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Add, null,
                                        tint = if (atStockLimit) Color.White.copy(alpha = 0.35f) else Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── Stock badge ───────────────────────────────────────────────────────────────

@Composable
fun StockBadge(stock: Int) {
    val state = stockState(stock)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Dot indicator
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(state.color)
        )
        Text(
            text = if (state == StockState.OUT) "نفذ المخزون"
                   else "${stock} ${state.label}",
            fontSize = 10.sp,
            color = state.color,
            fontWeight = FontWeight.SemiBold,
            lineHeight = 10.sp
        )
    }
}
