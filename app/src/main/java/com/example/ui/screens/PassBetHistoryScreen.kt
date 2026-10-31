@file:OptIn(ExperimentalMaterial3Api::class)
package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.PassBetRecordEntity
import com.example.ui.MainViewModel
import com.example.util.copyPassBetsRecordToClipboard

/**
 * Standard PassBetHistoryTable component
 * Renders the 4-column Table UI with Oldest-First order:
 * [ စဉ် ] (0.9f) | [ အကြိမ် ] (1.2f) | [ တင်ကွက်ငွေပမာဏ ] (1.4f) | [ ကူးမည် ] (0.7f)
 */
@Composable
fun PassBetHistoryTable(
    records: List<PassBetRecordEntity>,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val sortedRecords = remember(records) {
        records.sortedWith(compareBy<PassBetRecordEntity> { it.recordNo }.thenBy { it.id })
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("တင်ကွက်မှတ်တမ်း", fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF235347))
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFFF7F9FA))
                .padding(12.dp)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // TABLE HEADER: စဉ် | အကြိမ် | တင်ကွက်ငွေပမာဏ | ကူးမည်
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF235347))
                            .padding(vertical = 10.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "စဉ်",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            modifier = Modifier.weight(0.9f),
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "အကြိမ်",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            modifier = Modifier.weight(1.2f),
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "တင်ကွက်ငွေပမာဏ",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            modifier = Modifier.weight(1.4f),
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "ကူးမည်",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            modifier = Modifier.weight(0.7f),
                            textAlign = TextAlign.Center
                        )
                    }

                    if (sortedRecords.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "တင်ကွက်မှတ်တမ်း မရှိသေးပါ",
                                color = Color(0xFF94A3B8),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            itemsIndexed(sortedRecords, key = { _, r -> r.id }) { idx, record ->
                                val displayNo = if (record.recordNo > 0) record.recordNo else (idx + 1)
                                val domainRecord = record.toDomainLayoffRecord()

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            copyPassBetsRecordToClipboard(
                                                context = context,
                                                record = domainRecord,
                                                recordNo = displayNo
                                            )
                                        }
                                        .padding(vertical = 10.dp, horizontal = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // 1. စဉ် Badge
                                    Box(
                                        modifier = Modifier.weight(0.9f),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Surface(
                                            color = Color(0xFFFFEB3B),
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                text = "No. $displayNo",
                                                color = Color.Black,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }

                                    // 2. အကြိမ်
                                    Text(
                                        text = record.sessionName,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = Color(0xFF235347),
                                        modifier = Modifier.weight(1.2f),
                                        textAlign = TextAlign.Center
                                    )

                                    // 3. တင်ကွက်ငွေပမာဏ
                                    Text(
                                        text = "${record.totalAmount}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = Color(0xFF111111),
                                        modifier = Modifier.weight(1.4f),
                                        textAlign = TextAlign.Center
                                    )

                                    // 4. ကူးမည် Action Button
                                    Box(
                                        modifier = Modifier.weight(0.7f),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        IconButton(
                                            onClick = {
                                                copyPassBetsRecordToClipboard(
                                                    context = context,
                                                    record = domainRecord,
                                                    recordNo = displayNo
                                                )
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.ContentCopy,
                                                contentDescription = "Copy Pass Bet",
                                                tint = Color(0xFF235347),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                                HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PassBetHistoryScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    LayoffHistoryScreen(viewModel = viewModel, modifier = modifier)
}
