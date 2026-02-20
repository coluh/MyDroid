package com.destywen.mydroid.ui.components

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.destywen.mydroid.data.local.ChatAgent

@Composable
fun AgentCard(agent: ChatAgent, onClick: () -> Unit = {}) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(null, LocalIndication.current, onClick = {
                onClick()
            }),
        backgroundColor = MaterialTheme.colors.secondary,
        elevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {

                Text(agent.name, fontWeight = FontWeight.Bold, lineHeight = 1.sp)
                Text(
                    agent.modelName,
                    fontSize = 14.sp,
                    lineHeight = 1.sp,
                    fontWeight = FontWeight.Bold
                )

            }
            Text(
                agent.systemPrompt,
                fontSize = 14.sp,
                maxLines = 1,
                softWrap = false,
                lineHeight = 1.sp,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                agent.endpoint,
                fontSize = 12.sp,
                softWrap = false,
                maxLines = 1,
                lineHeight = 1.sp
            ) // TODO: simplify these
        }
    }
}