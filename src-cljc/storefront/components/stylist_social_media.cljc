(ns storefront.components.stylist-social-media
  (:require [clojure.string :as string]
            [storefront.component :as component :refer [defcomponent]]
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

(defn ^:private copy [& sentences]
  (string/join " " sentences))

(defn ^:private vertical-blackline-atom
  [class]
  [:div.flex.justify-center
   {:class class}
   (svg/vertical-blackline
    {:style {:height "85px"}})])

(defn ^:private content-example
  [{:keys [title img-uuid content target]}]
  [:div.grid.overflow-hidden.mb4
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
    [:div.flex.mx3.flex-column-on-mb
     (ui/img {:src   "55276115-9ba6-4a25-aa9e-b511e5a72186"
              :alt   ""
              :class "my2"
              :style {:max-width "600px"}})
     [:div.m3
      [:div.proxima.content-2.left-align.center-align-on-mb
       (copy "With your video submission for each look,"
             "include short notes via email with any processes and products that you used,"
             "as well as custom color details (if applicable).")]
      [:h3.proxima.title-1.bold.my2.shout.left-align "Examples of Content"]
      [:ul.content-2.proxima.left-align.my2
       [:li "Lifted hair with 20 volume lightener"]
       [:li "Colored with 10 volume Goldwell 6N & 6B"]
       [:li "Toned with Colerance 6B"]
       [:li "Added layers to shape the style and customized the hairline on the closure."]]
      [:h3.proxima.title-1.bold.my2.shout.left-align.mx3 "Note:"]
      [:div.proxima.content-2.left-align.mx3.center-align-on-mb
       "Do not include any text, supers, overlays, watermarks, or heavy filtering on your videos."
       "We will add any on-crean notes needed during editing. Include (via email) which username you'd like us to credit."]]]
    [:h3.proxima.title-1.bold.my2.shout "Remember"]
    [:div.proxima.content-2.mb8
     "These notes allow us to showcase both your skillset and the range of capabilities that are possible with Mayvenn products."]]
   (vertical-blackline-atom nil)
   [:h2.title-1.canela.my8 "Content Examples"]
   [:div.gap-2.m2.flex.flex-column-on-mb.mb6
    (content-example {:title    "Timelapse"
                      :img-uuid "b831b10c-1a34-445f-85db-a3a6777625cb"
                      :content  "Videos that show the entire install and styling process"
                      :target   nil})
    (content-example {:title    "Transitional"
                      :img-uuid "e74c9cbe-1601-4adf-bbe0-a8b484b3a572"
                      :content  "Videos that have fun transitions of before and afters"
                      :target   nil})
    (content-example {:title    "Transformational"
                      :img-uuid "07eb4bb3-ad04-47e0-9a29-26b95c064021"
                      :content  "Videos that show transformations of the process or a before and after"
                      :target   nil})
    (content-example {:title    "Client-Focused"
                      :img-uuid "07eb4bb3-ad04-47e0-9a29-26b95c064021"
                      :content  "Videos that focus on the client's experience and reviews"
                      :target   nil})]
   (vertical-blackline-atom "mbn8")
   [:h2.bg-warm-gray.canela.title-1.py10
    [:div "We can't wait to see your artistry in action!"]
    [:div "Thank you!"]]])

(defn built-component [data opts]
  (component/build component (query data) nil))
