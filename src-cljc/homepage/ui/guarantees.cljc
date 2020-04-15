(ns homepage.ui.guarantees
  (:require [storefront.component :as c]))

(defn ^:private guarantees-icon-molecule
  [{:guarantees.icon/keys [image title body]} id]
  (c/html
   [:div.pb1.pt6.col-6-on-tb-dt
    {:key id}
    [:div image]
    [:div.title-2.proxima.py1.shout title]
    [:p.content-2.py1.col-9-on-tb-dt.mx-auto body]]))

(defn ^:private icons-list-molecule
  [{:list/keys [icons]} breakpoint]
  (for [[idx element] (map-indexed vector icons)
        :let [id (str "guarantees.icon" breakpoint idx)]]
    (guarantees-icon-molecule element id)))

(def ^:private guarantees-title-molecule
  [:div.mt5.mb3
   [:h2.title-1.proxima.shout.pb1
    [:div.img-logo.bg-no-repeat.bg-center.bg-contain
     {:style {:height "29px"}}]]
   [:div.title-1.canela.shout "guarantees"]])

(c/defcomponent organism
  [{:as data :keys [icons]} _ _]
  [:div.center.bg-cool-gray.p6-on-dt
   [:div.col-12.flex.flex-column.items-center.py6
    guarantees-title-molecule
    [:div.col-8.flex.flex-column.items-center.hide-on-dt
     (icons-list-molecule data :mobile)]
    [:div.col-10.flex.flex-wrap.justify-between.hide-on-mb-tb
     (icons-list-molecule data :desktop)]]])
