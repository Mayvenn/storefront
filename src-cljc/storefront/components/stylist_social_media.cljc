(ns storefront.components.stylist-social-media
  (:require [storefront.component :as component :refer [defcomponent]]
            [storefront.components.money-formatters :as mf]
            [storefront.components.svg :as svg]
            [storefront.components.ui :as ui]
            [storefront.accessors.contentful :as contentful]
            [adventure.components.layered :as layered]
            [storefront.keypaths :as keypaths]
            [storefront.events :as events]
            [storefront.platform.component-utils :as utils]
            [storefront.routes :as routes]
            [storefront.ugc :as ugc]
            [spice.maps :as maps]
            [storefront.components.homepage-hero :as homepage-hero]
            [ui.molecules :as ui.M]))

(defn ^:private content-example
  [{:keys [title img-uuid content target]}]
  [:div.grid.overflow-hidden
   {:style {:grid-template-rows "auto 500px 120px 30px"
            :flex               1}}
   [:h3.proxima.title-1 title]
   (ui/img {:src   img-uuid
            :alt   ""
            :class "container-size"
            :style {:object-fit "cover"}})
   [:div.bg-pale-purple.proxima.content-1.flex.items-center.justify-around.px3
    {:style {:grid-row "3 / 4"}}
    content]
   [:div.container-size.flex.items-center.justify-around
    {:style {:grid-row "4 / 5"}}
    (ui/button-small-underline-primary {:href target}
                                       "Link to Post")]])

(defn query [data] {})

(defcomponent component [data owner opts]
  [:div.center
   (component/build ui.M/fullsize-image {:alt       ""
                                         :file-name "stylist-social-media.jpg"
                                         :dsk-uuid  "61412360-ac86-4f1d-88c7-a61d77e2ef59"
                                         :mob-uuid  "7eb2d06a-1817-464d-a31a-35b1ba4ec600"
                                         :ucare?    true}
                    nil)
   [:h1.proxima.bold.content-1.shout.my3 "Stylist Content Partnership"]
   [:h2.title-1.canela.my3 "Deliverable Details"]
   [:div.mx3
    [:div.proxima.content-2.m3 "CONTENT MISSING"]
    [:div.hide-on-mb-tb.flex.mx3
     (ui/img {:src   "55276115-9ba6-4a25-aa9e-b511e5a72186"
              :alt   ""
              :class "col-6 my2"})
     [:div
      [:div.proxima.content-2.my2.left-align "CONTENT MISSING"]
      [:h3.proxima.title-1.bold.m3.shout.left-align "Examples of Content"]
      [:ul.content-2.proxima.left-align.my2
       [:li "Lifted hair with 20 volume lightener"]
       [:li "Colored with 10 volume Goldwell 6N & 6B"]
       [:li "Toned with Colerance 6B"]
       [:li "Added layers to shape the style and customized the hairline on the closure."]]
      [:h3.proxima.title-1.bold.my2.shout.left-align.mx3 "Note:"]
      [:div.proxima.content-2.left-align.mx3 "Do not include any text, supers, overlays, watermarks, or heavy filtering on your videos. "
       "We will add any on-crean notes needed during editing. Include (via email) which username you'd like us to credit."]]]
    [:div.hide-on-dt
     (ui/img {:src   "55276115-9ba6-4a25-aa9e-b511e5a72186"
              :alt   ""
              :class "col-8 my2"})
     [:div.proxima.content-2.my2 "CONTENT MISSING"]
     [:h3.proxima.title-1.bold.my3 "Examples of Content"
      [:ul.content-2.proxima.left-align.my2
       [:li "Lifted hair with 20 volume lightener"]
       [:li "Colored with 10 volume Goldwell 6N & 6B"]
       [:li "Toned with Colerance 6B"]
       [:li "Added layers to shape the style and customized the hairline on the closure"]]]
     [:h3.proxima.title-1.bold.my2 "Note:"]
     [:div.proxima.content-2 "Do not include any text, supers, overlays, watermarks, or heavy filtering on your videos."
      "We will add any on-crean notes needed during editing. Include (via email) which username you'd like us to credit."]]
    [:h3.proxima.title-1.bold.my2.shout "Remember"]
    [:div.proxima.content-2
     "These notes allow us to showcase both your skillset and the range of capabilities that are possible with Mayvenn products."]]
   [:h2.title-1.canela.my8 "Content Examples"]
   [:div.gap-2.mx2.flex.flex-column-on-mb
    (content-example {:title    "Timelapse"
                      :img-uuid "8527101d-24a6-4729-ab33-cc808c350118"
                      :content  "Videos that show the entire install and styling process"
                      :target   nil})
    (content-example {:title    "Transitional"
                      :img-uuid "9dda1c3f-6b2b-4c9c-9f81-65c774297655"
                      :content  "Videos that have fun transitions of before and afters"
                      :target   nil})
    (content-example {:title    "Transformational"
                      :img-uuid "68ca7848-c81f-4adb-b8d0-fe8c03e3d7de"
                      :content  "Videos that show transformations of the process or a before and after"
                      :target   nil})
    (content-example {:title    "Client Focused"
                      :img-uuid "c7fabb3b-2c5a-4351-a93b-8f1d92fbc932"
                      :content  "Videos that focus on the client's experience and reviews"
                      :target   nil})]
   [:h2.bg-warm-gray.canela.title-2.center.py4
    [:div "We can't wait to see your artistry in action!"]
    [:div "Thank you!"]]])

(defn built-component [data opts]
  (component/build component (query data) nil))
