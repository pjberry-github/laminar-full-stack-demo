package com.raquo.app.integrations

import com.raquo.airstream.core.Transaction
import com.raquo.app.JsRouter.titleLink
import com.raquo.app.codesnippets.CodeSnippets
import com.raquo.laminar.api.L.{*, given}
import com.raquo.laminar.codecs.StringAsIsCodec
import com.raquo.laminar.shoelace.sl
import com.raquo.utils.Utils.useImport
import org.scalajs.dom

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object ShoelaceWebComponentsView {

  @js.native @JSImport("@find/**/ShoelaceWebComponentsView.less", JSImport.Namespace)
  private object Stylesheet extends js.Object

  useImport(Stylesheet)

  // Load Shoelace themes. Light one is the default, but we make a button to switch them.
  // See their contents at https://github.com/shoelace-style/shoelace/blob/current/src/themes/light.css
  // BEGIN[shoelace/themes]
  @js.native @JSImport("@shoelace-style/shoelace/dist/themes/light.css", "importStyle")
  private def importLightTheme(): Unit = js.native

  importLightTheme()

  @js.native @JSImport("@shoelace-style/shoelace/dist/themes/dark.css", "importStyle")
  private def importDarkTheme(): Unit = js.native

  importDarkTheme()
  // END[shoelace/themes]

  // This path is determined by `dest` config of `rollupCopyPlugin` in vite.config.js
  // Note: This needs to be called once, prior to loading any Shoelace components
  //   JsApp would be a good place to put this, I'm just putting it here because
  //   in this demo project, this is the only view that uses Shoelace components.
  sl.Shoelace.setBasePath("/assets/shoelace")

  def apply(): HtmlElement = {
    // BEGIN[shoelace/themes]
    val isDarkVar = Var(false)
    // END[shoelace/themes]

    div(
      cls("ShoelaceWebComponentsView"),
      //
      h1("Shoelace Web Components"),
      p(a(href("https://shoelace.style/"), "Shoelace"), " is a well made library of modern looking Web Components."),
      p(a(href("https://github.com/raquo/laminar-shoelace-components"), "Laminar Shoelace bindings"), " are currently in their early stages, but they're quite usable enough to render most Shoelace components, including everything below!"),
      p(b("TODO: "), "CodeSnippets in this demo should support loading files from external URLs, otherwise I can't show implementation of web components"),

      // CodeSnippets(
      //   _.`shoelace/components`.sortBy {
      //     case s if s.fileName.endsWith("Button.scala") => 1
      //     case s if s.fileName.endsWith("Icon.scala") => 2
      //     case s if s.fileName.endsWith("Switch.scala") => 3
      //     case s if s.fileName.endsWith("WebComponent.scala") => 4
      //     case s if s.fileName.endsWith("CommonTypes.scala") => 5
      //     case _ => 10
      //   },
      //   caption = "Source code of my Shoelace interfaces used below, for reference:",
      //   asParagraph = true,
      //   startExpanded = _ => false
      // ),

      h2("Controlled inputs", titleLink("controlled-inputs")),
      p("Laminar supports ", a("controlled inputs", href("https://laminar.dev/documentation#controlled-inputs")), " on Web Components."), //
      {
        // BEGIN[shoelace/controlled-inputs]
        val zipVar = Var("")
        def isValidInput(str: String): Boolean = str.length <= 5 && str.forall(_.isDigit)
        p(
          display.flex,
          alignItems.end,
          styleProp("gap")("20px"),
          sl.Input.of(
            _.label("Enter 5 digit zip code:"),
            _ => width.px(200),
            _.controlled(
              _.value <-- zipVar,
              _.onInput.mapToValue.filter(isValidInput) --> zipVar,
            )
          ),
          sl.Button.of(
            _.variant.warning,
            _.on(onClick).mapTo("91403") --> zipVar,
            _ => "Set to SF"
          ),
          div("zipVar content: ", text <-- zipVar)
        )
        // END[shoelace/controlled-inputs]
      },
      CodeSnippets(_.`shoelace/controlled-inputs`),
      //
      h2("Buttons and Icons", titleLink("buttons-icons")),
      p(
        // BEGIN[shoelace/buttons-and-icons]
        sl.Button.of(
          _.variant.primary,
          _.size.large,
          _ => "Settings",
          _ => onClick --> { _ => dom.window.alert("Clicked") },
          _.slots.prefix(
            sl.Icon.of(
              _.name("gear-fill"),
              _ => fontSize.em(1.3), // this is how you set icon size in shoelace
            )
          ),
          _.slots.suffix(
            child <-- EventStream.periodic(3000, resetOnStop = false).map(n => span(" " + n % 10))
          )
        ),
        " ",
        sl.Button.of(
          _ => "Reload",
          _ => onClick --> { _ => dom.window.alert("Clicked") },
          _.slots.prefix(
            sl.Icon.of(_.name("arrow-counterclockwise"))
          )
        ),
        " ",
        sl.Button.of(
          _.variant.success,
          _ => "User",
          _ => onClick --> { _ => dom.window.alert("Clicked") },
          _.slots.suffix(
            sl.Icon.of(_.name("person-fill"))
          )
        )
        // END[shoelace/buttons-and-icons]
      ),
      p("This example also demonstrates the usage of named slots."),
      p("Icons and their names are from ", a(href("https://icons.getbootstrap.com"), "Bootstrap Icons"), " by default. To find available icons, create a search engine bookmark in your browser with keyword ", code("bs"), " and URL ", code("https://icons.getbootstrap.com/?q=%s"), ", then you'll be able to type e.g. \"bs user\" in your address bar, and see all relevant icons and their names."),
      CodeSnippets(_.`shoelace/buttons-and-icons`),
      //
      h1("Customization"),
      //
      p("These methods and examples largely follow ", a(href("https://shoelace.style/getting-started/customizing"), "Shoelace customization docs"), "."),
      //
      h2("Using themes", titleLink("themes")),
      // BEGIN[shoelace/themes]
      cls <-- isDarkVar.signal.map(if (_) "sl-theme-dark" else "sl-theme-light"),
      // END[shoelace/themes]
      // BEGIN[shoelace/themes]
      sl.Button.of(
        _.variant.primary,
        _ => text <-- isDarkVar.signal.map(if (_) "Use light theme" else "Use dark theme"),
        _ => onClick.mapTo(!isDarkVar.now()) --> isDarkVar,
        _.slots.prefix(
          sl.Icon.of(
            _.name <-- isDarkVar.signal.map(if (_) "brightness-high-fill" else "moon-stars-fill"),
            _ => fontSize.em(1.3), // this is how you set icon size in shoelace
          )
        )
      ),
      // END[shoelace/themes]
      p("The button above switches this page between light and dark themes. The themes style the Shoelace components that we render here, but I also set the color and background color of this page by referring to the CSS vars that the active theme sets."),
      p("See ", a(href("https://shoelace.style/getting-started/themes"), "Shoelace theme docs"), " for instructions on using themes, creating your own themes, and loading multiple themes in the same app."),
      CodeSnippets(_.`shoelace/themes`),
      //
      h2("Using design tokens", titleLink("design-tokens")),
      // BEGIN[shoelace/design-tokens]
      p(
        cls("indigoPrimaryColor"), // you could also apply this class directly to the button.
        sl.Button.of(
          _.variant.primary,
          _ => "Primary indigo",
          _ => onClick --> { _ => dom.window.alert("Clicked") }
        )
      ),
      // END[shoelace/design-tokens]
      p("Shoelace theme defines \"design tokens\", which are just CSS custom properties. These properties are inherited, so you can override those either globally or only in a certain CSS scope. The button above is rendered using \"primary\" style, but we overrode the primary colors to be indigo instead of the default sky blue."),
      CodeSnippets(_.`shoelace/design-tokens`),
      //
      h2("Using CSS parts", titleLink("css-parts")),
      p(
        // BEGIN[shoelace/css-parts]
        sl.Button.of(
          _ => cls("tomato-button"),
          _ => "Tasteful tomato button",
          _ => onClick --> { _ => dom.window.alert("Clicked") },
          _.slots.prefix(
            sl.Icon.of(_.name("check-circle-fill"))
          )
        ),
        " ",
        sl.Button.of(
          _ => cls("pink"),
          _ => "Crazy pink button",
          _ => onClick --> { _ => dom.window.alert("Clicked") }
        ),
        // END[shoelace/css-parts]
        CodeSnippets(_.`shoelace/css-parts`)
      ),
      //
      h2("Using CSS custom properties", titleLink("css-custom-properties")),
      p(
        // #TODO[IJ] Why isn't the right `Switch` offered for import here? File a bug report after publishing the repo.
        // BEGIN[shoelace/css-custom-properties]
        sl.Switch.of(),
        " ",
        sl.Switch.of(
          _ => width.px(100),
          _ => height.px(10),
          _.thumbSize.px(14)
        ),
        // END[shoelace/css-custom-properties]
        CodeSnippets(_.`shoelace/css-custom-properties`)
      ),
      p("Above is a standard ", a(href("https://shoelace.style/components/switch"), "Switch"), " component, followed by a Switch that is custom-sized using custom CSS properties that this web component exposes."),
    )
  }
}
