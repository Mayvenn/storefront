(ns storefront.components.gallery-edit
  (:require #?@(:cljs [react
                       [storefront.api :as api]])
            [storefront.component :as component :refer [defcomponent]]
            [storefront.components.ui :as ui]
            [storefront.effects :as effects]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.platform.component-utils :as utils]))

(def add-photo-square
  [:a.block.col-4.pp1.bg-pale-purple.white
   (merge (utils/route-to events/navigate-gallery-image-picker)
          {:data-test "add-to-gallery-link"})
   (ui/aspect-ratio 1 1
                    [:div.flex.flex-column.justify-evenly.container-size
                     [:div ui/nbsp]
                     [:div.center.bold
                      {:style {:font-size "60px"}}
                      "+"]
                     [:div.center.shout.title-3.proxima "Add Photo"]])])

(def pending-approval
  (component/html
   [:div.container-size.bg-gray.flex.items-center.center.p2.proxima.content-2
    "Your image has been successfully submitted and is pending approval. Check back here to be updated on its status."]))

(defcomponent static-component [{:keys [gallery]} owner opts]
  [:div.container
   (into [:div.clearfix.mxn1.flex.flex-wrap
          add-photo-square]
         (for [{:keys [status resizable-url id]} gallery]
           [:a.col-4.pp1.inherit-color
            (merge (utils/route-to events/navigate-gallery-photo {:photo-id id})
                   {:key resizable-url})
            (ui/aspect-ratio 1 1
                             (if (= "approved" status)
                               (ui/img {:class                         "container-size"
                                        :style                         {:object-position "50% 25%"
                                                                        :object-fit      "cover"}
                                        :preserve-url-transformations? true
                                        :src                           resizable-url
                                        :max-size                      749})
                               pending-approval))]))])

#?(:cljs (defn do-nothing-handler [e]
           (.preventDefault e)
           (.stopPropagation e)))

#?(:cljs (defn view-mode-attrs [photo-id]
           (when photo-id
             (utils/route-to events/navigate-gallery-photo {:photo-id photo-id}))))

#?(:cljs
   (defn base-container-attrs [post-id]
     {:data-post-id    post-id
      :on-context-menu do-nothing-handler
      :style           {:padding "1px"}}))

(defn query [state]
  {:gallery (get-in state keypaths/user-stylist-gallery-images)})
