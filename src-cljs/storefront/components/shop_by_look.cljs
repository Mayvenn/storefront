(ns storefront.components.shop-by-look
  (:require [om.core :as om]
            [sablono.core :refer-macros [html]]
            [storefront.accessors.pixlee :as pixlee]
            [storefront.platform.ugc :as ugc]
            [storefront.components.ui :as ui]
            [storefront.keypaths :as keypaths]))

(defn image-thumbnail [img]
  [:img.col-12.block img])

(defn image-attribution [look]
  [:div.bg-light-gray.p1
   [:div.h5.mt1.mb2.mx3-on-mb.mx1-on-tb-dt
    (ugc/user-attribution look)]
   (ugc/view-look-button look)])

(defn component [{:keys [looks]} owner opts]
  (om/component
   (html
    [:div
     [:div.center.bg-light-gray.py3
      [:h1.h2.navy "shop by look"]
      [:div.img-shop-by-look-icon.bg-no-repeat.bg-contain.mx-auto.my2
       {:style {:width "101px" :height "85px"}} ]
      [:p.dark-gray.col-10.col-6-on-tb-dt.mx-auto "Get inspired by #MayvennMade community. Find your favorite look and click it to easily add it to your bag!"]]
     [:div.container.clearfix.mtn2
      (for [{:keys [id imgs] :as look} looks]
        [:div
         {:key id}
         [:div.py2.col-12.col.hide-on-tb-dt {:key (str "small-" id)}
          (image-thumbnail (:medium imgs))
          (image-attribution look)]
         [:div.p2.col.col-4.hide-on-mb {:key (str "large-" id)}
          (ui/aspect-ratio
           1 1
           {:class "hoverable"}
           (image-thumbnail (:medium imgs))
           [:div.absolute.bottom-0.col-12.show-on-hover (image-attribution look)])]])]])))

(defn query [data]
  (let [looks (->> (pixlee/images-in-album (get-in data keypaths/ugc) :mosaic)
                   (remove (comp #{"video"} :content-type)))]
    {:looks looks}))

(defn built-component [data opts]
  (om/build component (query data) opts))
