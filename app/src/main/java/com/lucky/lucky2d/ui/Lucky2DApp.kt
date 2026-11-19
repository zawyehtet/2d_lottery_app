package com.lucky.lucky2d.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.lucky.lucky2d.security.LicenseManager
import com.lucky.lucky2d.ui.screens.ActivationScreen
import com.lucky.lucky2d.ui.screens.AgentSettlementScreen
import com.lucky.lucky2d.ui.screens.AgentsScreen
import com.lucky.lucky2d.ui.screens.BrakeScreen
import com.lucky.lucky2d.ui.screens.HomeScreen
import com.lucky.lucky2d.ui.screens.LayoffHistoryScreen
import com.lucky.lucky2d.ui.screens.NumberEntryScreen
import com.lucky.lucky2d.ui.screens.NumbersScreen
import com.lucky.lucky2d.ui.screens.PasswordLockScreen
import com.lucky.lucky2d.ui.screens.SettingsScreen
import com.lucky.lucky2d.ui.screens.SettlementScreen
import com.lucky.lucky2d.ui.screens.SplashScreen
import com.lucky.lucky2d.ui.screens.VouchersScreen
import com.lucky.lucky2d.ui.theme.LuckyBg
import com.lucky.lucky2d.ui.theme.LuckyGreenDark
import com.lucky.lucky2d.viewmodel.LotteryViewModel

object Routes {
    const val HOME = "home"
    const val AGENTS = "agents"
    const val NUMBER_ENTRY = "number_entry"
    const val VOUCHERS = "vouchers"
    const val NUMBERS = "numbers"
    const val BRAKE = "brake"
    const val LAYOFF_HISTORY = "layoff_history"
    const val SETTLEMENT = "settlement"
    const val AGENT_SETTLEMENT = "agent_settlement"
    const val SETTINGS = "settings"
}

@Composable
fun Lucky2DApp(viewModel: LotteryViewModel = viewModel()) {
    val context = LocalContext.current
    var showSplash by remember { mutableStateOf(true) }
    var isActivated by remember { mutableStateOf(LicenseManager.isAppActivated(context)) }
    var isUnlocked by remember {
        mutableStateOf(
            !(viewModel.settings.passwordLockEnabled && viewModel.settings.appPassword.isNotBlank())
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LuckyBg)
    ) {
        when {
            showSplash -> {
                SplashScreen(
                    onFinish = {
                        showSplash = false
                    }
                )
            }

            !isActivated -> {
                ActivationScreen(
                    onActivated = {
                        isActivated = true
                    }
                )
            }

            viewModel.settings.passwordLockEnabled &&
                viewModel.settings.appPassword.isNotBlank() &&
                !isUnlocked -> {
                PasswordLockScreen(
                    expectedPassword = viewModel.settings.appPassword,
                    onUnlocked = {
                        isUnlocked = true
                    }
                )
            }

            else -> {
                val navController = rememberNavController()
                val safeBack: () -> Unit = {
                    if (navController.previousBackStackEntry != null) {
                        navController.popBackStack()
                    }
                }

                NavHost(
                    navController = navController,
                    startDestination = Routes.HOME,
                    modifier = Modifier.fillMaxSize()
                ) {
                    composable(Routes.HOME) {
                        HomeScreen(
                            viewModel = viewModel,
                            onOpenAgents = {
                                navController.navigate(Routes.AGENTS)
                            },
                            onOpenNumbers = {
                                navController.navigate(Routes.NUMBERS)
                            },
                            onOpenVouchers = {
                                viewModel.clearVoucherFilter()
                                navController.navigate(Routes.VOUCHERS)
                            },
                            onOpenBrake = {
                                navController.navigate(Routes.BRAKE)
                            },
                            onOpenSettings = {
                                navController.navigate(Routes.SETTINGS)
                            }
                        )
                    }

                    composable(Routes.AGENTS) {
                        AgentsScreen(
                            viewModel = viewModel,
                            onBack = safeBack,
                            onOpenNumberEntry = { agent ->
                                viewModel.startNumberEntry(agent)
                                navController.navigate(Routes.NUMBER_ENTRY)
                            },
                            onOpenAgentVouchers = { agent ->
                                viewModel.openAgentVouchersFilter(agent.id, agent.name)
                                navController.navigate(Routes.VOUCHERS)
                            },
                            onOpenSettings = {
                                navController.navigate(Routes.SETTINGS)
                            }
                        )
                    }

                    composable(Routes.NUMBER_ENTRY) {
                        NumberEntryScreen(
                            viewModel = viewModel,
                            onClose = safeBack,
                            onOpenAgentVouchers = {
                                val agent = viewModel.entryAgent
                                viewModel.openAgentVouchersFilter(agent?.id, agent?.name)
                                navController.navigate(Routes.VOUCHERS)
                            }
                        )
                    }

                    composable(Routes.VOUCHERS) {
                        VouchersScreen(
                            viewModel = viewModel,
                            onBack = safeBack,
                            onOpenSettings = {
                                navController.navigate(Routes.SETTINGS)
                            }
                        )
                    }

                    composable(Routes.NUMBERS) {
                        NumbersScreen(
                            viewModel = viewModel,
                            onBack = safeBack,
                            onOpenSettings = {
                                navController.navigate(Routes.SETTINGS)
                            }
                        )
                    }

                    composable(Routes.BRAKE) {
                        BrakeScreen(
                            viewModel = viewModel,
                            onBack = safeBack,
                            onOpenSettlement = {
                                navController.navigate(Routes.SETTLEMENT)
                            },
                            onOpenLayoffHistory = {
                                navController.navigate(Routes.LAYOFF_HISTORY)
                            },
                            onOpenSettings = {
                                navController.navigate(Routes.SETTINGS)
                            }
                        )
                    }

                    composable(Routes.LAYOFF_HISTORY) {
                        LayoffHistoryScreen(
                            viewModel = viewModel,
                            onBack = safeBack
                        )
                    }

                    composable(Routes.SETTLEMENT) {
                        SettlementScreen(
                            viewModel = viewModel,
                            onBack = safeBack
                        )
                    }

                    composable(Routes.AGENT_SETTLEMENT) {
                        AgentSettlementScreen(
                            viewModel = viewModel,
                            onBack = safeBack,
                            onOpenSettings = {
                                navController.navigate(Routes.SETTINGS)
                            }
                        )
                    }

                    composable(Routes.SETTINGS) {
                        SettingsScreen(
                            viewModel = viewModel,
                            onBack = safeBack,
                            onOpenAgentSettlement = {
                                navController.navigate(Routes.AGENT_SETTLEMENT)
                            }
                        )
                    }
                }
            }
        }

        // Global bottom toast pill overlay
        val currentToast = viewModel.toastMessage
        AnimatedVisibility(
            visible = currentToast != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 22.dp)
        ) {
            Surface(
                color = LuckyGreenDark.copy(alpha = 0.96f),
                shape = RoundedCornerShape(999.dp),
                shadowElevation = 6.dp
            ) {
                Text(
                    text = currentToast.orEmpty(),
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp)
                )
            }
        }
    }
}
