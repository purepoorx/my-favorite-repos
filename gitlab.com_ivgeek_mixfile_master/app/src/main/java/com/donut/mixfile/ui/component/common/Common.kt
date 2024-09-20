package com.donut.mixfile.ui.component.common

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.outlined.Done
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.donut.mixfile.util.truncate

@Composable
fun CommonColumn(
    modifier: Modifier = Modifier,
    verticalArrangement: Arrangement.HorizontalOrVertical = Arrangement.spacedBy(8.dp),
    horizontalAlignment: Alignment.Horizontal = Alignment.CenterHorizontally,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier,
//            .padding(8.dp),
        verticalArrangement = verticalArrangement,
        horizontalAlignment = horizontalAlignment,
        content = content
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LabelSwitch(
    checked: Boolean,
    label: String,
    onCheckedChangeListener: (Boolean) -> Unit = {},
) {
    FlowRow(
        modifier = Modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = label,
            modifier = Modifier.align(Alignment.CenterVertically)
        )
        Switch(
            checked = checked,
            onCheckedChange = {
                onCheckedChangeListener(it)
            },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CommonSwitch(
    checked: Boolean,
    text: String,
    description: String = "",
    useDivider: Boolean = true,
    onCheckedChangeListener: (Boolean) -> Unit = {},
) {

    Column {
        if (useDivider) {
            HorizontalDivider()
        }
        FlowRow(
            modifier = Modifier
                .fillMaxWidth(),
//                .padding(10.dp, 0.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(text = text, modifier = Modifier.align(Alignment.CenterVertically))
            Switch(
                checked = checked,
                onCheckedChange = {
                    onCheckedChangeListener(it)
                },
            )
        }
        if (description.isNotEmpty()) {
            Text(
                text = description,
                modifier = Modifier
                    .fillMaxWidth(),
//                    .padding(10.dp, 0.dp),
                color = Color(0xFF9E9E9E),
                fontSize = 14.sp
            )
        }
    }
}

@Composable
fun ClearableTextField(
    value: TextFieldValue,
    modifier: Modifier = Modifier,
    maxLines: Int = Int.MAX_VALUE,
    label: String = "",
    onValueChange: (TextFieldValue) -> Unit,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.Bottom
    ) {
        TextField(
            value = value,
            maxLines = maxLines,
            onValueChange = {
                onValueChange(it)
            },
            label = { Text(text = label) },
//            textStyle = TextStyle(color = Color.Black), // 可以根据需要设置文本样式
            modifier = Modifier.weight(1f) // 占据剩余空间
        )

        if (value.text.isNotEmpty()) {
            Icon(
                Icons.Filled.Clear,
                contentDescription = "Clear text",
                tint = colorScheme.primary,
                modifier = Modifier
                    .clickable {
                        onValueChange(TextFieldValue())
                    }
                    .padding(4.dp)
            )
        }
    }
}

@Composable
fun SingleSelectItemList(
    items: List<String>,
    currentOption: String?,
    onSelect: (String) -> Unit,
) {
    SingleSelectItemList(
        items = items,
        currentOption = currentOption,
        getLabel = { it },
        onSelect = onSelect
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun <T> SingleSelectItemList(
    items: List<T>,
    currentOption: T?,
    getLabel: (option: T) -> String,
    onSelect: (option: T) -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .heightIn(0.dp, 400.dp),
    ) {
        items(items.size) { item ->
            val currentItem = items[item]
            val selected = currentOption == currentItem
            FilterChip(
                label = { Text(text = getLabel(currentItem).truncate(13)) },
                onClick = {
                    onSelect(currentItem)
                },

                selected = selected,
                leadingIcon = if (selected) {
                    {
                        Icon(
                            Icons.Outlined.Done,
                            contentDescription = "selected",
                            tint = colorScheme.primary
                        )
                    }
                } else {
                    null
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}