/*
 * Copyright 2019 Square Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.squareup.sample.helloworkflowfragment

import com.squareup.sample.helloworkflowfragment.HelloWorkflow.Rendering
import com.squareup.sample.helloworkflowfragment.HelloWorkflow.State
import com.squareup.sample.helloworkflowfragment.HelloWorkflow.State.Goodbye
import com.squareup.sample.helloworkflowfragment.HelloWorkflow.State.Hello
import com.squareup.workflow1.Snapshot
import com.squareup.workflow1.StatefulWorkflow
import com.squareup.workflow1.action
import com.squareup.workflow1.parse

object HelloWorkflow : StatefulWorkflow<Unit, State, Nothing, Rendering>() {
  enum class State {
    Hello,
    Goodbye
  }

  data class Rendering(
    val items: List<String>,
    val message: String,
    val onClick: () -> Unit
  )

  override fun initialState(
    props: Unit,
    snapshot: Snapshot?
  ): State = snapshot?.bytes?.parse { source -> if (source.readInt() == 1) Hello else Goodbye }
    ?: Hello

  override fun render(
    props: Unit,
    state: State,
    context: RenderContext
  ): Rendering {
    return Rendering(
      items = DUMMY_ITEMS,
      message = state.name,
      onClick = { context.actionSink.send(helloAction) }
    )
  }

  override fun snapshotState(state: State): Snapshot = Snapshot.of(if (state == Hello) 1 else 0)

  private val helloAction = action {
    state = when (state) {
      Hello -> Goodbye
      Goodbye -> Hello
    }
  }
}

private val DUMMY_ITEMS = listOf(
  "a", "b", "c", "d", "e", "f", "g"
)
