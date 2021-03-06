/*
 * Copyright 2020 Square Inc.
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
@file:JvmMultifileClass
@file:JvmName("Workflows")

package com.squareup.workflow1

import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlin.reflect.KType

/**
 * The [Workflow] that implements the logic for actually running [Worker]s.
 *
 * This workflow is an [ImpostorWorkflow] and uses the entire [KType] of the [Worker] as its
 * [realIdentifier], so that the runtime can ensure that distinct worker types are allowed to run
 * concurrently. Implements [Worker.doesSameWorkAs] by taking the actual worker instance as its
 * props, and checking [Worker.doesSameWorkAs] in [onPropsChanged]. When this returns false, it
 * means a new worker session needs to be started, and that is achieved by storing a monotonically-
 * increasing integer as the state, and incrementing it whenever the worker needs to be restarted.
 *
 * Note that since this workflow uses an [unsnapshottableIdentifier] as its [realIdentifier], it is
 * not snapshottable, but that's fine because the only state this workflow maintains is only used
 * to determine whether to restart workers during the lifetime of a single runtime instance.
 *
 * @param workerType The [KType] representing the particular type of `Worker<OutputT>`.
 * @param key The key used to render this workflow, as passed to [RenderContext.runningWorker].
 * Used for naming the worker's coroutine.
 */
@OptIn(ExperimentalWorkflowApi::class)
internal class WorkerWorkflow<OutputT>(
  val workerType: KType,
  private val key: String
) : StatefulWorkflow<Worker<OutputT>, Int, OutputT, Unit>(),
    ImpostorWorkflow {

  override val realIdentifier: WorkflowIdentifier = unsnapshottableIdentifier(workerType)
  override fun describeRealIdentifier(): String? = "worker $workerType"

  override fun initialState(
    props: Worker<OutputT>,
    snapshot: Snapshot?
  ): Int = 0

  override fun onPropsChanged(
    old: Worker<OutputT>,
    new: Worker<OutputT>,
    state: Int
  ): Int = if (!old.doesSameWorkAs(new)) state + 1 else state

  override fun render(
    props: Worker<OutputT>,
    state: Int,
    context: RenderContext
  ) {
    // Scope the side effect coroutine to the state value, so the worker will be re-started when
    // it changes (such that doesSameWorkAs returns false above).
    context.runningSideEffect(state.toString()) {
      runWorker(props, key, context.actionSink)
    }
  }

  override fun snapshotState(state: Int): Snapshot? = null
}

/**
 * Does the actual running of a worker passed to [RenderContext.runningWorker] by setting up the
 * coroutine environment for the worker, performing some validation, etc., and finally actually
 * collecting the worker's [Flow].
 *
 * Visible for testing.
 */
@OptIn(ExperimentalWorkflowApi::class)
internal suspend fun <OutputT> runWorker(
  worker: Worker<OutputT>,
  renderKey: String,
  actionSink: Sink<WorkflowAction<Worker<OutputT>, Int, OutputT>>
) {
  withContext(CoroutineName(worker.debugName(renderKey))) {
    worker.runWithNullCheck()
        .collectToSink(actionSink) { output ->
          EmitWorkerOutputAction(worker, renderKey, output)
        }
  }
}

private class EmitWorkerOutputAction<P, S, O>(
  private val worker: Worker<*>,
  private val renderKey: String,
  private val output: O
) : WorkflowAction<P, S, O>() {
  override fun toString(): String =
    "${EmitWorkerOutputAction::class.qualifiedName}(worker=$worker, key=\"$renderKey\")"

  override fun Updater.apply() {
    setOutput(output)
  }
}

/**
 * In unit tests, if you use a mocking library to create a Worker, the run method will return null
 * even though the return type is non-nullable in Kotlin. Kotlin helps out with this by throwing an
 * NPE before before any kotlin code gets the null, but the NPE that it throws includes an almost
 * completely useless stacktrace and no other details.
 *
 * This method does an explicit null check and throws an exception with a more helpful message.
 *
 * See [#842](https://github.com/square/workflow/issues/842).
 */
@Suppress("USELESS_ELVIS")
private fun <T> Worker<T>.runWithNullCheck(): Flow<T> =
  run() ?: throw NullPointerException(
      "Worker $this returned a null Flow. " +
          "If this is a test mock, make sure you mock the run() method!"
  )

private fun Worker<*>.debugName(key: String) =
  toString().let { if (key.isBlank()) it else "$it:$key" }
