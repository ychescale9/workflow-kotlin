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
@file:Suppress("EXPERIMENTAL_API_USAGE")

package com.squareup.workflow

import com.squareup.workflow.StatefulWorkflow.RenderContext
import com.squareup.workflow.WorkflowAction.Companion.noAction
import com.squareup.workflow.WorkflowAction.Updater


/**
 * Convenience alias of [RenderContext.renderChild] for workflows that don't take props.
 */
/* ktlint-disable parameter-list-wrapping */
fun <StateT, OutputT : Any, ChildOutputT : Any, ChildRenderingT>
    RenderContext<StateT, OutputT>.renderChild(
  child: Workflow<Unit, ChildOutputT, ChildRenderingT>,
  key: String = "",
  handler: (ChildOutputT) -> WorkflowAction<StateT, OutputT>
): ChildRenderingT = renderChild(child, Unit, key, handler)
/* ktlint-enable parameter-list-wrapping */

/**
 * Convenience alias of [RenderContext.renderChild] for workflows that don't take props or emit
 * output.
 */
/* ktlint-disable parameter-list-wrapping */
fun <PropsT, StateT, OutputT : Any, ChildRenderingT>
    RenderContext<StateT, OutputT>.renderChild(
  child: Workflow<PropsT, Nothing, ChildRenderingT>,
  props: PropsT,
  key: String = ""
): ChildRenderingT = renderChild(child, props, key) { noAction() }
/* ktlint-enable parameter-list-wrapping */

/**
 * Convenience alias of [RenderContext.renderChild] for workflows that don't take props or emit
 * output.
 */
/* ktlint-disable parameter-list-wrapping */
fun <StateT, OutputT : Any, ChildRenderingT>
    RenderContext<StateT, OutputT>.renderChild(
  child: Workflow<Unit, Nothing, ChildRenderingT>,
  key: String = ""
): ChildRenderingT = renderChild(child, Unit, key) { noAction() }
/* ktlint-enable parameter-list-wrapping */

/**
 * Ensures a [Worker] that never emits anything is running. Since [worker] can't emit anything,
 * it can't trigger any [WorkflowAction]s.
 *
 * A simple way to create workers that don't output anything is using [Worker.createSideEffect].
 *
 * @param key An optional string key that is used to distinguish between identical [Worker]s.
 */
fun <StateT, OutputT : Any> RenderContext<StateT, OutputT>.runningWorker(
  worker: Worker<Nothing>,
  key: String = ""
) {
  // Need to cast to Any so the compiler doesn't complain about unreachable code.
  runningWorker(worker as Worker<Any>, key) {
    throw AssertionError("Worker<Nothing> emitted $it")
  }
}

/**
 * Alternative to [RenderContext.actionSink] that allows externally defined
 * event types to be mapped to anonymous [WorkflowAction]s.
 */
fun <EventT, StateT, OutputT : Any> RenderContext<StateT, OutputT>.makeEventSink(
  update: Updater<StateT, OutputT>.(EventT) -> Unit
): Sink<EventT> = actionSink.contraMap { event ->
  action({ "eventSink($event)" }) { update(event) }
}

/**
 * Ensures [worker] is running. When the [Worker] emits an output, [handler] is called
 * to determine the [WorkflowAction] to take. When the worker finishes, nothing happens (although
 * another render pass may be triggered).
 *
 * @param key An optional string key that is used to distinguish between identical [Worker]s.
 */
@Deprecated(
    "Use runningWorker",
    ReplaceWith("runningWorker(worker, key, handler)", "com.squareup.workflow.runningWorker")
)
fun <StateT, OutputT : Any, T> RenderContext<StateT, OutputT>.onWorkerOutput(
  worker: Worker<T>,
  key: String = "",
  handler: (T) -> WorkflowAction<StateT, OutputT>
) = runningWorker(worker, key, handler)
