# Mayvenn Storefront
[The front of shop.mayvenn.com][9].

# NOTE: This document represents our goals, not necessarily our current state.

## Overview
Storefront is built primarily using ClojureScript and Om, with a smidgen of Clojure.

## Directory Structure
Storefront's source is first split into three src directories: `src-clj`, `src-cljs`, `src-cljc`.
Naturally, these dirs correspond to the language files contained within them.
The differences between the three files will be talked about more in a later section,
for now let's refer to the top level as `src*`.

The next layer of directories corelates to modules.
A module generally corresponds to a single feature or logical area of storefront.
More on those in a bit.

Inside each module, there are
components (representing a single react component of the site, both appearance and behavior),
hooks (direct integration with javascript),
accessors (transformation functions for commonly used datastructure),
a single routes file (which defines that module's routes)
and a core file (which simply imports all of the components for that module to make importing all cljs components easier).


## Modules
There are 9 modules: core, account, catalog, checkout, dashboard, gallery, home, leads, login.

### Core
This is the frame of storefront.
It is the 'glue' which ties all of the modules together and it contains some utilities which are necessary across all modules.
If code needs to be in more than one module, it probably goes into this module

### Account
This module contains the code involved in editing a user or stylist's information

### Catalog
This module contains the code involved in shopping.

### Checkout
This module contains the code involved in checking out.

### Dashboard
This module contains the code involved in the stylist dashboard and the cash out now code.

### Gallery
This module contains the code involved in stylist gallery

### Home
This module contains the code involved in home page and static content

### Leads
This module contains the code involved in the welcome page and leads page

### Login
This module contains the code involved in logging in.

## Architecture
Storefront's is built around a single application state and 5 event systems.
An event is a vector of keywords.

### Events
Events are evaluated in a cascading manner, for example, examine the event `navigate-checkout-returning-or-guest`.
It evaluates to `[:navigate :checkout :returning :or :guest]`.

#### Multimethods
There are many multimethods for which events can have implementations.
They are listed here in order of execution.

##### Transitions
```clojure

(defmulti transition-state
  (fn [dispatch event arguments app-state]
    dispatch))

```

If a given event has a transition defmethod registered, then that code will execute and update application state.
It should not be side-effectful and it must always return the application's state

##### Effects
If a given event has a effect defmethod registered, then that code will execute perform side-effectful operations
such as API calls, external library operations or interactions with cookies.
It should not have any side effects and it must always return the application's state.
Effects can also dispatch further events.

##### Query
All components should implement a query defmethod.
Query is responsible for transforming data in app-state into the shape that the component needs.

##### Display
All components should implement a display defmethod.
Display is responsible for building the component.

##### Trackings

#### Executing Events
As the function handle-message processes the event for most event multimethods,
it executes left to right, building as it goes.
The exceptions are the Query and Display steps (we can only render one component for a given place at a time!).
`handle-message` finishes all event steps for a given defmethod before continuing.

So, it would execute the following events:
1. Transition
  1. `[:navigate]`
  2. `[:navigate :checkout]`
  3. `[:navigate :checkout :returning]`
  4. `[:navigate :checkout :returning :or]`
  5. `[:navigate :checkout :returning :or :guest]`
2. Effect
  1. `[:navigate]`
  2. `[:navigate :checkout]`
  3. `[:navigate :checkout :returning]`
  4. `[:navigate :checkout :returning :or]`
  5. `[:navigate :checkout :returning :or :guest]`
3. Query
  1. `[:navigate :checkout :returning :or :guest]`
4. Display
  1. `[:navigate :checkout :returning :or :guest]`
5. Trackings
  1. `[:navigate]`
  2. `[:navigate :checkout]`
  3. `[:navigate :checkout :returning]`
  4. `[:navigate :checkout :returning :or]`
  5. `[:navigate :checkout :returning :or :guest]`


Note that not all intermediate events need to have defmultis implemented.
Some examples would be `[:navigate :checkout :returning]` and `[:navigate :checkout :returning :or]`.

#### Examples:
##### Getting data asynchronously over an API
##### Configuring a component to update app state when interacted with.



## Server Side Rendering

[Here is some more information about effects and transitions][8]




## Questions
* Where are the tests?
  * The vast majority of Storefront's tests are in a different project.
  This was done because they are integration tests and test more than just storefront.
  There are some tests for the server side handler.
* Can I run this?
  * Sure, but you will need to setup up some external dependencies, such as an API server. We hope this project serves as a reference project moreso than generic ecommerce solution.

## License
Copyright (C) Mayvenn, Inc. - All Rights Reserved
