package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.CustomerWithBalance
import com.example.localsync.model.PeerDevice
import com.example.ui.util.CurrencyFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainLedgerScreen(
    customers: List<CustomerWithBalance>,
    searchQuery: String,
    totalShopDebt: Double,
    discoveredPeers: List<PeerDevice>,
    onSearchQueryChange: (String) -> Unit,
    onCustomerClick: (String) -> Unit,
    onAddCustomerClick: () -> Unit,
    onImportBalancesClick: () -> Unit,
    onImportContactsClick: () -> Unit,
    onNetworkStatusClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "دفتر الديون المحلي",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "مزامنة تلقائية بدون إنترنت (Wi-Fi)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = onImportBalancesClick,
                        modifier = Modifier.testTag("import_balances_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.FileUpload,
                            contentDescription = "استيراد من ملف",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    IconButton(
                        onClick = onImportContactsClick,
                        modifier = Modifier.testTag("import_contacts_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Contacts,
                            contentDescription = "استيراد من جهات الاتصال",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    IconButton(
                        onClick = onNetworkStatusClick,
                        modifier = Modifier.testTag("network_status_button")
                    ) {
                        BadgedBox(
                            badge = {
                                if (discoveredPeers.isNotEmpty()) {
                                    Badge(containerColor = MaterialTheme.colorScheme.tertiary) {
                                        Text(CurrencyFormatter.convertToArabicNumerals("${discoveredPeers.size}"))
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = if (discoveredPeers.isNotEmpty()) Icons.Default.Wifi else Icons.Default.WifiOff,
                                contentDescription = "حالة الشبكة",
                                tint = if (discoveredPeers.isNotEmpty()) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddCustomerClick,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("add_customer_fab")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.PersonAdd, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "زبون جديد",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Total Debt Header Stats Banner
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "إجمالي ديون المحل (على الزبائن)",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = CurrencyFormatter.formatAmount(totalShopDebt),
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = CurrencyFormatter.convertToArabicNumerals("${customers.size}"),
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "زبون مسجل",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = { Text("بحث باسم الزبون أو رقم الهاتف...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "مسح")
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .testTag("search_customer_input")
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Customer List
            if (customers.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        androidx.compose.foundation.Image(
                            painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.daftar_hero_illustration_1785003608977),
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxWidth(0.8f)
                                .clip(RoundedCornerShape(16.dp))
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = if (searchQuery.isBlank()) "لا يوجد زبائن مسجلين حالياً" else "لم يتم العثور على نتائج للبحث",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.outline
                        )
                        if (searchQuery.isBlank()) {
                            Text(
                                text = "اضغط على زر (+) لإضافة أول زبون",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                ) {
                    items(customers, key = { it.customer.id }) { item ->
                        CustomerListItem(
                            customerWithBalance = item,
                            onClick = { onCustomerClick(item.customer.id) }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun CustomerListItem(
    customerWithBalance: CustomerWithBalance,
    onClick: () -> Unit
) {
    val customer = customerWithBalance.customer
    val balance = customerWithBalance.netBalance
    val isDebt = balance > 0
    val isSettled = balance == 0.0

    val badgeBg = if (isDebt) MaterialTheme.colorScheme.errorContainer else if (isSettled) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.tertiaryContainer
    val badgeTextColor = if (isDebt) MaterialTheme.colorScheme.onErrorContainer else if (isSettled) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onTertiaryContainer

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("customer_item_${customer.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Circle Avatar Initials
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = customer.name.take(1),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = customer.name,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (customer.phone.isNotBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Phone,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = MaterialTheme.colorScheme.outline
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = customer.phone,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                    Text(
                        text = CurrencyFormatter.formatDate(customerWithBalance.lastTransactionTime),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }

            // Balance Badge
            Card(
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = badgeBg)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = if (isDebt) "مطلوب" else if (isSettled) "متسدد" else "دائن",
                        style = MaterialTheme.typography.labelSmall,
                        color = badgeTextColor
                    )
                    Text(
                        text = CurrencyFormatter.formatAmount(kotlin.math.abs(balance)),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = badgeTextColor
                    )
                }
            }
        }
    }
}
