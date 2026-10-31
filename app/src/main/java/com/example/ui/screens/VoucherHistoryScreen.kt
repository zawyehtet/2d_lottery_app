package com.example.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.ui.MainViewModel

/**
 * VoucherHistoryScreen alias mapping to VouchersScreen
 */
@Composable
fun VoucherHistoryScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    VouchersScreen(viewModel = viewModel, modifier = modifier)
}

@Composable
fun VoucherHistoryScreen2D(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    VouchersScreen(viewModel = viewModel, modifier = modifier)
}

@Composable
fun VoucherListScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    VouchersScreen(viewModel = viewModel, modifier = modifier)
}
