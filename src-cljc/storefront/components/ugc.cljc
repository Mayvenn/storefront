(ns storefront.components.ugc
  (:require  [storefront.platform.component-utils :as util]
             [storefront.components.ui :as ui]
             [storefront.components.svg :as svg]
             #?@(:clj [[storefront.component-shim :as component]]
                 :cljs [[storefront.component :as component]
                        [goog.string]])))

(defn user-attribution [{:keys [user-handle social-service]}]
  [:div.flex.items-center
   [:div.flex-auto.dark-gray.medium {:style {:word-break "break-all"}} "@" user-handle]
   [:div.ml1.line-height-1 {:style {:width "1em" :height "1em"}}
    (svg/social-icon social-service)]])

(defn view-look-button [{{:keys [view-look view-other]} :links} button-copy nav-stack-item]
  (let [[nav-event nav-args] (or view-look view-other)]
    (ui/teal-button
     (util/route-to nav-event nav-args nav-stack-item)
     button-copy)))

(defn image-thumbnail [img]
  [:img.col-12.block img])

(defn image-attribution [look {:keys [button-copy back-copy short-name]}]
  [:div.bg-light-gray.p1
   [:div.h5.mt1.mb2.mx3-on-mb.mx1-on-tb-dt
    (user-attribution look)]
   (view-look-button look button-copy {:back-copy back-copy
                                       :short-name short-name})])

(defn content-view [{:keys [imgs content-type source-url] :as item}]
  (ui/aspect-ratio
   1 1
   {:class "bg-black"}
   (if (= content-type "video")
     [:video.container-size.block {:controls true}
      [:source {:src source-url}]]
     [:div.container-size.bg-cover.bg-no-repeat.bg-center
      {:style {:background-image (str "url(" (-> imgs :large :src) ")")}}])))

(defn component [{:keys [looks]} owner {:keys [copy]}]
  (component/create
   [:div.container.clearfix.mtn2
    (for [{:keys [id imgs] :as look} looks]
      [:div
       {:key id}
       [:div.py2.col-12.col.hide-on-tb-dt {:key (str "small-" id)}
        (image-thumbnail (:medium imgs))
        (image-attribution look copy)]
       [:div.p2.col.col-4.hide-on-mb {:key (str "large-" id)}
        (ui/aspect-ratio
         1 1
         {:class "hoverable"}
         (image-thumbnail (:medium imgs))
         [:div.absolute.bottom-0.col-12.show-on-hover (image-attribution look copy)])]])]))
