# Mayvenn Storefront
[The front of shop.mayvenn.com][9].

## Overview
Storefront is built primarily using ClojureScript and Om, with a smidgen of Clojure.

## Server Side
On the server side, there is a simple static file server with some redirects and a very tiny bit of dynamic code (for robots and stuff).

## Client Architecture
Storefront's Om code is built around a single application state and two disparate [events][2] systems, [transistions][0] and [effects][1].
Events processed in transitions mutate state (adding product information after it has been retrieved),
while those processed in effects cause side-effects (making api calls and causing animations).
Dispite being separate, the two event systems operate on the same events.
This allows a clean separation of concerns.
Additionally, events are collections of sub-events, which are each run separately.
This allows for some common tasks to be abstracted cleanly.

For example, lets say the [navigate-product][3] event is triggered.
It is a concatenation of event navigate and a new keyword ```:product```, making the whole event ```[:navigate :product]```.
The event first propagates to [transitions][5] the :navigate event is run.
Then, the :navigate :product event is run, assoc'ing in the necessary page navigation states into the app state.
Afterwards the event is processed by [effects][6] where an API call to fetch the product is started.
Upon success, a callback that triggers the [api-success-product][4] is called. 
That api success event is then processed by [transitions][7] and the product is moved into the application state.
Via the magic of Om (and therefore react) bindings, the product information is bound from the application state.


[Here is some more information about effects and transistions][8]




## Questions
* Where are the tests?
  * The vast majority of Storefront's tests are in a different project.
  This was done because they are integration tests and test more than just storefront.
* Whats up with your SCSS and CSS?
  * They are from the previous version of the site.
* Can I run this?
  * You have our permission, but not our support :smile:


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
