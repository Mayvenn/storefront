(ns homepage.ui.shopping-categories
  (:require [clojure.string :refer [join split]]
            [storefront.component :as c]
            [storefront.components.ui :as ui]
            [storefront.platform.component-utils :as utils]))

(defn ^:private shopping-categories-image-atom
  "Assumptions: 2 up on mobile, 3 up on tablet/desktop, within a .container.
  Does not account for 1px border."
  [image-id filename alt]
  [:img.block.col-12.container-size
   {:src     (str "//ucarecdn.com/" image-id "/-/format/auto/-/resize/640x/-/quality/lightest/" filename)
    :src-set (str "//ucarecdn.com/" image-id "/-/format/auto/-/resize/640x/-/quality/lightest/" filename " 640w, "
                  "//ucarecdn.com/" image-id "/-/format/auto/-/resize/544x/-/quality/lightest/" filename " 544w")
    :sizes   "100%"
    :alt     alt
    :style {:object-fit "cover"}}])

(defn ^:private shopping-categories-label-atom
  [label]
  (let [[first-word & rest-of-words :as words]
        (split label #" ")]
    [:div.absolute.black.bottom-0.shout.ml1
     [:div.proxima.content-4.bold
      first-word [:br] (join " " rest-of-words)]]))

(defn ^:private shopping-categories-alt-label-atom
  [[first-line last-line]]
  [:div.absolute.black.bottom-0.shout.ml1
   [:div.hide-on-mb-tb.proxima.content-3.bold
    first-line [:br] last-line]
   [:div.hide-on-dt.proxima.content-4.bold
    first-line [:br] last-line]])

(defn ^:private shopping-categories-box-molecule
  [{:shopping-categories.box/keys [id target ucare-id label alt-label]} react-key]
  (let [height 210
        width  171]
    [:div.col.col-6.px1.my1
     {:key react-key :data-test id}
     [:div.bg-pale-purple
      (ui/aspect-ratio width height
                       [:a (apply utils/route-to target)
                        (when ucare-id
                          (shopping-categories-image-atom ucare-id
                                                          id
                                                          (or label (join alt-label))))
                        (if label
                          (shopping-categories-label-atom label)
                          (shopping-categories-alt-label-atom alt-label))])]]))

(defn ^:private boxes-list-molecule
  [{:list/keys [boxes]}]
  (for [[idx element] (map-indexed vector boxes)
        :let [id (str "shopping-categories.box" idx)]]
    (shopping-categories-box-molecule element id)))

(c/defcomponent organism
  [data _ _]
  (when (seq data)
    [:div.px2.myj1
     [:div.container.hide-on-tb-dt ; mobile
      (boxes-list-molecule data)]
     [:div.col-10-on-dt.container.hide-on-mb.flex ; desktop
      (let [{:list/keys [boxes]} data]
        (boxes-list-molecule {:list/boxes (butlast boxes)}))]]))
