# Mayvenn Storefront
[The front of shop.mayvenn.com][9].

## Overview
Storefront is built primarily using ClojureScript and Om, with a smidgen of Clojure.

## Server Side
On the server side, there is a simple static file server with some redirects and a very tiny bit of dynamic code (for robots and stuff).

## Client Architecture
Storefront's Om code is built around a single application state and two disparate [events][2] systems, [transitions][0] and [effects][1].
Events processed in transitions mutate state, e.g. adding product information after it has been retrieved.
On the other hand, those processed in effects cause side-effects, such as making api calls and causing animations.
Despite being separate, the two event systems operate on the same events.
This allows a clean separation of concerns.
Events are collections of keywords that transitions and effects operate on [reductions][10] of these collections.
This allows for some common tasks to be abstracted cleanly.

For example, lets say the [navigate-category][3] event, equivalent to ```[:navigate :category]``` is triggered:
 1. The event will propagate to [transitions][11] and the ```:navigate``` transition will run.
 2. Still in [transitions][5], the transition for ```[:navigate :category]``` will run, and some page nav information will be moved into the app state.
 3. The event will propagate to [effects][12] and the ```:navigate``` effect will run.  It will start API calls for various information that needs to be loaded on every page change.
 4. Still in [effects][6], the ```[:navigate :category]``` effect will execute and an API call to fetch the category is started.
 5. (**ASYNC**) Upon success, a callback that triggers the [api-success-category][4] is called.  The other API calls from step 3 have similar functionality, so lets skip them for now.
 6. The api success event is then processed by [transitions][7] and the category is moved into the application state.
 7. Via the magic of Om (and therefore react) bindings, the category information is bound from the application state.


[Here is some more information about effects and transitions][8]

## ClojureScript Directory Structure
Starting at [/src-cljs/storefront/][13], there are some directories.

* Accessors - Utility modules for working with our domain models
* Browser - Modules which work with browser APIs directly, e.g. scrolling and cookies
* Components - React UI components
* Hooks - Things like middleware or 3rd party code which is non-essential to the operation of storefront e.g. yotpo, bugsnag
* Utils - Small helper libraries that are generic in nature, e.g. a Uuid generator


## Questions
* Where are the tests?
  * The vast majority of Storefront's tests are in a different project.
  This was done because they are integration tests and test more than just storefront.
* Whats up with your SCSS and CSS?
  * They are from the previous version of the site.  Going forward we would like to make the SCSS more component oriented.
* Can I run this?
  * Sure, but you will need to setup up some external dependencies, such as an API server. We hope this project serves as a reference project moreso than generic ecommerce solution.

## License
Copyright (C) Mayvenn, Inc. - All Rights Reserved

[0]: https://github.com/Mayvenn/storefront/blob/master/src-cljs/storefront/transitions.cljs
[1]: https://github.com/Mayvenn/storefront/blob/master/src-cljs/storefront/effects.cljs
[2]: https://github.com/Mayvenn/storefront/blob/master/src-cljs/storefront/events.cljs
[3]: https://github.com/Mayvenn/storefront/blob/master/src-cljs/storefront/events.cljs#L10
[4]: https://github.com/Mayvenn/storefront/blob/master/src-cljs/storefront/events.cljs#L107
[5]: https://github.com/Mayvenn/storefront/blob/master/src-cljs/storefront/transitions.cljs#L26
[6]: https://github.com/Mayvenn/storefront/blob/master/src-cljs/storefront/effects.cljs#L79
[7]: https://github.com/Mayvenn/storefront/blob/master/src-cljs/storefront/transitions.cljs#L107
[8]: http://engineering.mayvenn.com/2015/05/28/Transitions-and-Effects/
[9]: https://shop.mayvenn.com
[10]: https://clojuredocs.org/clojure.core/reductions
[11]: https://github.com/Mayvenn/storefront/blob/master/src-cljs/storefront/transitions.cljs#L18
[12]: https://github.com/Mayvenn/storefront/blob/master/src-cljs/storefront/effects.cljs#L33
[13]: https://github.com/Mayvenn/storefront/tree/master/src-cljs/storefront
