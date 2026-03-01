package com.destywen.mydroid.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.primarySurface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.destywen.mydroid.data.local.AgentEntity
import com.destywen.mydroid.util.toDateTimeString

@Composable
fun AgentCard(agent: AgentEntity, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp)
    ) {
        var showDetail by rememberSaveable { mutableStateOf(false) }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(agent.name, fontWeight = FontWeight.Bold)
            Text("${agent.modelName}(温度${agent.temperature})", fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text(
                agent.systemPrompt,
                style = MaterialTheme.typography.body2,
                maxLines = if (showDetail) Int.MAX_VALUE else 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.clickable(null, LocalIndication.current) {
                    showDetail = !showDetail
                }
            )
            if (agent.apiEndpoint != null) {
                Text(agent.apiEndpoint, style = MaterialTheme.typography.caption)
            }
            Text(
                agent.createdAt.toDateTimeString(),
                style = MaterialTheme.typography.caption,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}