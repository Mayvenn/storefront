(ns catalog.ui.how-it-works
  (:require
   [storefront.component :as c]
   [storefront.components.svg :as svg]))

(defn ^:private how-it-works-title-molecule
  [{:how-it-works/keys [title-primary title-secondary]}]
  [:div
   [:div.canela.title-1 title-primary]
   [:div.proxima.title-1.shout title-secondary]])

(defn ^:private how-it-works-step-body-molecule
  [{:how-it-works.step.body/keys [primary]}]
  [:div.canela.content-2 primary])

(defn ^:private how-it-works-step-title-molecule
  [{:how-it-works.step.title/keys [primary secondary]}]
  [:div
   [:div.canela.title-2 primary]
   [:div.proxima.title-2.shout secondary]])

(def how-it-works-straight-line-atom
  [:div.stroke-s-color.pt4.hide-on-dt
   (svg/straight-line {:width  "1px"
                       :height "42px"})])

(c/defcomponent how-it-works-step-organism
  [step _ {:keys [id]}]
  [:div.py5
   {:key id}
   [:div.col-8.mx-auto
    (how-it-works-step-title-molecule step)]
   [:div.pt1
    (how-it-works-step-body-molecule step)]])

(defn ^:private elements
  "Embed a list of organisms in another organism."
  ([organism data elem-key]
   (elements organism data elem-key :default))
  ([organism data elem-key breakpoint]
   (let [elems (get data elem-key)]
     (for [[idx elem] (map-indexed vector elems)]
       (c/build organism
                elem
                (c/component-id elem-key
                                breakpoint
                                idx))))))

(c/defcomponent organism
  [data _ _]
  (when (seq data)
    [:div.center
     (how-it-works-title-molecule data)
     how-it-works-straight-line-atom
     (elements how-it-works-step-organism
               data
               :how-it-works/step-elements)]))
