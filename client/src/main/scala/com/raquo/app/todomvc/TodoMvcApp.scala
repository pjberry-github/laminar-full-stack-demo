package com.raquo.app.todomvc

import com.raquo.app.codesnippets.CodeSnippets
import com.raquo.laminar.api.L.{*, given}
import com.raquo.utils.Utils.useImport
import org.scalajs.dom

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

// BEGIN[todomvc]
object TodoMvcApp {

  @JSImport("@find/**/TodoMvcApp.css")
  @js.native private object Stylesheet extends js.Object

  useImport(Stylesheet)

  // This implementation is very loosely based on Outwatch TodoMVC, for comparison see
  // https://github.com/clovellytech/outwatch-examples/tree/master/todomvc/src/main/scala/todomvc


  // --- 1. Models ---

  case class TodoItem(id: Int, text: String, completed: Boolean)

  sealed abstract class Filter(val name: String, val passes: TodoItem => Boolean)

  object ShowAll extends Filter("All", _ => true)
  object ShowActive extends Filter("Active", !_.completed)
  object ShowCompleted extends Filter("Completed", _.completed)

  val filters: List[Filter] = ShowAll :: ShowActive :: ShowCompleted :: Nil

  sealed trait Command

  case class Create(itemText: String) extends Command
  case class UpdateText(itemId: Int, text: String) extends Command
  case class UpdateCompleted(itemId: Int, completed: Boolean) extends Command
  case class Delete(itemId: Int) extends Command
  case object DeleteCompleted extends Command


  // --- 2. State ---

  // Var-s are reactive state variables suitable for both local state and redux-like global stores.
  // Laminar uses my library Airstream as its reactive layer https://github.com/raquo/Airstream

  private val itemsVar = Var(List[TodoItem]())
  private val filterVar = Var[Filter](ShowAll)
  private var lastId = 1 // just for auto-incrementing IDs

  private val commandObserver = Observer[Command] {
    case Create(itemText) =>
      lastId += 1
      if (filterVar.now() == ShowCompleted)
        filterVar.set(ShowAll)
      itemsVar.update(_ :+ TodoItem(id = lastId, text = itemText, completed = false))
    case UpdateText(itemId, text) =>
      itemsVar.update(_.map(item => if (item.id == itemId) item.copy(text = text) else item))
    case UpdateCompleted(itemId, completed) =>
      itemsVar.update(_.map(item => if (item.id == itemId) item.copy(completed = completed) else item))
    case Delete(itemId) =>
      itemsVar.update(_.filterNot(_.id == itemId))
    case DeleteCompleted =>
      itemsVar.update(_.filterNot(_.completed))
      filterVar.set(ShowAll)
  }


  // --- 3. Views ---

  lazy val node: HtmlElement = {
    val todoItemsSignal = itemsVar
      .signal
      .combineWith(filterVar.signal)
      .mapN(_ filter _.passes)
    div(
      div(
        cls("todoapp-container u-bleed"),
        div(
          cls("todoapp"),
          div(
            cls("header"),
            h1("todos"),
            renderNewTodoInput,
          ),
          div(
            hideIfNoItems,
            cls("main"),
            ul(
              cls("todo-list"),
              children <-- todoItemsSignal.split(_.id)(renderTodoItem)
            )
          ),
          renderStatusBar
        )
      ),
      CodeSnippets(_.`todomvc`)
    )
  }

  private def renderNewTodoInput =
    input(
      cls("new-todo"),
      placeholder("What needs to be done?"),
      onMountFocus,
      onEnterPress
        .mapToValue
        .filter(_.nonEmpty)
        .map(Create(_))
        .setValue("") --> commandObserver,
      // When all we need is to clear an uncontrolled input, we can use setValue("")
      //  but we still need an observer to create the subscription, so we just use an empty one.
      onEscapeKeyUp.setValue("") --> Observer.empty
    )

  // Render a single item. Note that the result is a single element: not a stream, not some virtual DOM representation.
  private def renderTodoItem(itemId: Int, initialTodo: TodoItem, itemSignal: Signal[TodoItem]): HtmlElement = {
    val isEditingVar = Var(false) // Example of local state
    val updateTextObserver = commandObserver.contramap[UpdateText] { updateCommand =>
      isEditingVar.set(false)
      updateCommand
    }
    li(
      cls <-- itemSignal.map(item => Map("completed" -> item.completed)),
      onDblClick.filter(_ => !isEditingVar.now()).mapTo(true) --> isEditingVar.writer,
      children <-- isEditingVar.signal.map[List[HtmlElement]] {
        case true =>
          val cancelObserver = isEditingVar.writer.contramap[Unit](Unit => false)
          renderTextUpdateInput(itemId, itemSignal, updateTextObserver, cancelObserver) :: Nil
        case false =>
          List(
            renderCheckboxInput(itemId, itemSignal),
            label(text <-- itemSignal.map(_.text)),
            button(
              cls("destroy"),
              onClick.mapTo(Delete(itemId)) --> commandObserver
            )
          )
      }
    )
  }

  // Note that we pass reactive variables: `itemSignal` for reading, `updateTextObserver` for writing
  private def renderTextUpdateInput(
    itemId: Int,
    itemSignal: Signal[TodoItem],
    updateTextObserver: Observer[UpdateText],
    cancelObserver: Observer[Unit]
  ) =
    input(
      cls("edit"),
      defaultValue <-- itemSignal.map(_.text),
      onEscapeKeyUp.mapToUnit --> cancelObserver,
      onEnterPress.mapToValue.map(UpdateText(itemId, _)) --> updateTextObserver,
      onBlur.mapToValue.map(UpdateText(itemId, _)) --> updateTextObserver
    )

  private def renderCheckboxInput(itemId: Int, itemSignal: Signal[TodoItem]) =
    input(
      cls("toggle"),
      typ("checkbox"),
      checked <-- itemSignal.map(_.completed),
      onInput.mapToChecked.map { isChecked =>
        UpdateCompleted(itemId, completed = isChecked)
      } --> commandObserver
    )

  private def renderStatusBar =
    footerTag(
      hideIfNoItems,
      cls("footer"),
      span(
        cls("todo-count"),
        text <-- itemsVar.signal
          .map(_.count(!_.completed))
          .map(pluralize(_, "item left", "items left")),
      ),
      ul(
        cls("filters"),
        filters.map(filter => li(renderFilterButton(filter)))
      ),
      child.maybe <-- itemsVar.signal.map { items =>
        if (items.exists(ShowCompleted.passes)) Some(
          button(
            cls("clear-completed"),
            "Clear completed",
            onClick.map(_ => DeleteCompleted) --> commandObserver
          )
        ) else None
      }
    )

  private def renderFilterButton(filter: Filter) =
    a(
      cls("selected") <-- filterVar.signal.map(_ == filter),
      onClick.preventDefault.mapTo(filter) --> filterVar.writer,
      filter.name
    )

  // Every little thing in Laminar can be abstracted away
  private def hideIfNoItems: Mod[HtmlElement] =
    display <-- itemsVar.signal.map { items =>
      if (items.nonEmpty) "" else "none"
    }


  // --- Generic helpers ---

  private def pluralize(num: Int, singular: String, plural: String): String =
    s"$num ${if (num == 1) singular else plural}"

  private val onEnterPress = onKeyPress.filter(_.keyCode == dom.KeyCode.Enter)

  // Non-printable characters don't get a `keypress` event in JS,
  // so we need to listen to `keydown` or `keyup` instead.
  private val onEscapeKeyUp = onKeyUp.filter(_.keyCode == dom.KeyCode.Escape)
}
// END[todomvc]
