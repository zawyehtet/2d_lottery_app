package com.example.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.data.VoucherEntity
import com.example.ui.Lottery2DViewModel
import com.example.ui.MainViewModel

/**
 * VoucherHistoryScreen alias mapping to VouchersScreen and VoucherHistoryTableScreen2D
 */
@Composable
fun VoucherHistoryScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    VouchersScreen(viewModel = viewModel, modifier = modifier)
}
