(ns homepage.ui.shopping-categories
  (:require [clojure.string :refer [join split]]
            [storefront.component :as c]
            [storefront.components.ui :as ui]
            [storefront.platform.component-utils :as utils]))

(defn ^:private shopping-categories-image-atom
  "Assumptions: 2 up on mobile, 3 up on tablet/desktop, within a .container.
  Does not account for 1px border."
  [image-id filename alt]
  [:img.block.col-12
   {:src     (str "//ucarecdn.com/" image-id "/-/format/auto/-/resize/640x/-/quality/lightest/" filename)
    :src-set (str "//ucarecdn.com/" image-id "/-/format/auto/-/resize/640x/-/quality/lightest/" filename " 640w, "
                  "//ucarecdn.com/" image-id "/-/format/auto/-/resize/544x/-/quality/lightest/" filename " 544w")
    :sizes   "100%"
    :alt     alt}])

(defn ^:private shopping-categories-label-atom
  [label]
  (let [[first-word & rest-of-words :as words]
        (split label #" ")]
    [:div.absolute.white.bottom-0.shout.ml3.mb2
     [:div.hide-on-mb-tb.proxima.title-2
      (join " " words)]
     [:div.hide-on-dt.proxima.title-2
      first-word [:br] (join " " rest-of-words)]]))

(defn ^:private shopping-categories-alt-label-atom
  [[first-line last-line]]
  [:div.p2.flex.justify-around.items-center.bg-pale-purple.dark-gray.inherit-color.canela.title-2.center
   {:style {:height "100%"
            :width  "100%"}}
   first-line [:br] last-line])

(defn ^:private shopping-categories-box-molecule
  [{:shopping-categories.box/keys [id target ucare-id label alt-label]} react-key]
  (let [height 210
        width  171]
    [:div.col.col-6.col-4-on-tb-dt.px1.my1
     {:key react-key :data-test id}
     (ui/aspect-ratio width height
                      [:a (apply utils/route-to target)
                       (when ucare-id
                         (shopping-categories-image-atom ucare-id
                                                         id
                                                         (or label (join alt-label))))
                       (if label
                         (shopping-categories-label-atom label)
                         (shopping-categories-alt-label-atom alt-label))])]))

(defn ^:private boxes-list-molecule
  [{:list/keys [boxes]}]
  (for [[idx element] (map-indexed vector boxes)
        :let [id (str "shopping-categories.box" idx)]]
    (shopping-categories-box-molecule element id)))

(defn ^:private shopping-categories-title-molecule
  [{:shopping-categories.title/keys [primary]}]
  [:div.my3.center.mx-auto.title-1.canela primary])

(c/defcomponent organism
  [data _ _]
  [:div.p2.pb2.my5
   (shopping-categories-title-molecule data)
   [:div.col-8-on-dt.container
    (boxes-list-molecule data)]])
