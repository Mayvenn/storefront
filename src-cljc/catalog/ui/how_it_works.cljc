(ns catalog.ui.how-it-works
  (:require [storefront.component :as c]
            [storefront.components.svg :as svg]))

(defn ^:private how-it-works-title-molecule
  [{:how-it-works/keys [title-primary title-secondary]}]
  (c/html
   [:div
    [:div.canela.title-1 title-primary]
    [:div.proxima.title-1.shout title-secondary]]))

(defn ^:private how-it-works-step-body-molecule
  [{:how-it-works.step.body/keys [primary]}]
  (c/html
   [:div.canela.content-2 primary]))

(defn ^:private how-it-works-step-title-molecule
  [{:how-it-works.step.title/keys [primary secondary]}]
  (c/html
   [:div
    [:div.canela.title-2 primary]
    [:div.proxima.title-2.shout secondary]]))

(def how-it-works-straight-line-atom
  (c/html
   [:div.stroke-s-color.pt4.hide-on-dt
    (svg/straight-line {:width  "1px"
                        :height "42px"})]))

(c/defcomponent how-it-works-step-organism
  [step _ {:keys [id]}]
  [:div.py5.col-10.mx-auto
   {:key id}
   [:div
    (how-it-works-step-title-molecule step)]
   [:div.pt1
    (how-it-works-step-body-molecule step)]])

(c/defcomponent organism
  [data _ _]
  (when-let [how-it-works (:how-it-works data)]
    [:div.center.mbj3.px5-on-dt.mx-auto
     (how-it-works-title-molecule how-it-works)
     how-it-works-straight-line-atom
     (c/elements how-it-works-step-organism
                 how-it-works
                 :how-it-works/step-elements)]))
