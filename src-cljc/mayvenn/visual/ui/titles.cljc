(ns mayvenn.visual.ui.titles
  "
  Defined design elements around titles.

  What's a title? Right now:

  Any text/content box with the following data:
  {id icon primary secondary tertiary}

  TODO(corey)
  consider a combinator approach, e.g.:
  (def canela-small
       (text-box
         [{:text/value primary
           :pos/h :center
           :pos/v top
           :font/face :canela
           :font/size :title-3}]))

  "
  (:require [storefront.component :as c]
            [storefront.components.svg :as svg]))

(defn proxima
  "Usages:
  - call out boxes"
  [{:keys [id icon primary secondary]}]
  (c/html
   [:div.center
    (when icon
      (svg/symbolic->html icon))
    [:div.title-2.proxima.shout
     (when id
       {:data-test id})
     primary]
    (when secondary
      [:div.mt2.content-2
       secondary])]))

(defn proxima-small
  "Usages:
  - call out boxes"
  [{:keys [id icon primary secondary]}]
  (c/html
   [:div.center
    (when icon
      (svg/symbolic->html icon))
    [:div.title-3.proxima.shout
     (when id
       {:data-test id})
     primary]
    (when secondary
      [:div.mt2.content-2
       secondary])]))

(defn proxima-left
  "Usages:
  - stylist cards
  - product summary on product details"
  [{:keys [id icon primary secondary]}]
  (c/html
   [:div.left-align
    (when icon
      (svg/symbolic->html icon))
    [:div.title-2.proxima.shout
     (when id
       {:data-test id})
     primary]
    (when secondary
      [:div.mt2.content-2
       secondary])]))

(defn promixa-tiny-right
  [{:keys [id icon primary secondary]}]
  (c/html
   [:div.proxima.right-align
    {:data-test id}
    [:div.content-3 primary]
    [:div.content-4 secondary]]))

(defn proxima-small-left
  "Usages:
  - Top Stylist badge"
  [{:keys [id icon primary secondary]}]
  (c/html
   [:div.left-align
    [:div.proxima.title-3.flex.flex-auto
     (when icon
       (svg/symbolic->html icon))
     [:div
      (when id
        {:data-test id})
      primary]]
    (when secondary
      [:div.mt2.content-2
       secondary])]))

(defn proxima-content
  [{:keys [id icon primary secondary tertiary]}]
  (when (and id primary)
    [:div
     [:div.proxima.content-2
      {:data-test id}
      primary]
     [:div secondary]
     (if (vector? tertiary)
       (interpose [:br] tertiary)
       tertiary)]))

(defn canela
  "Usages:
  - call out boxes"
  [{:keys [id icon primary secondary]}]
  (c/html
   [:div.center
    (when icon
      (svg/symbolic->html icon))
    [:div.title-2.canela
     (when id
       {:data-test id})
     (if (vector? primary)
       (interpose [:br] primary)
       primary)]
    (when secondary
      [:div.mt2.content-3
       secondary])]))

(defn canela-left
  "Usages:
  - call out boxes"
  [{:keys [id icon primary secondary]}]
  (c/html
   [:div.left-align
    (when icon
      (svg/symbolic->html icon))
    [:div.title-2.canela
     (when id
       {:data-test id})
     primary]
    (when secondary
      [:div.mt2.content-3
       secondary])]))

(defn canela-huge
  "Usages:
  - call out boxes"
  [{:keys [id icon primary secondary]}]
  (c/html
   [:div.center
    (when icon
      [:div.myj1
       (svg/symbolic->html icon)])
    [:div.title-1.canela.myj1
     (when id
       {:data-test id})
     (if (vector? primary)
       (interpose [:br] primary)
       primary)]
    (when secondary
      [:div.content-2.myj1
       secondary])]))

