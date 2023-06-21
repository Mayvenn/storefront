(ns mayvenn.concept.persona
  (:require #?@(:cljs
                [[storefront.hooks.stringer :as stringer]
                 [storefront.api :as api]
                 [storefront.hooks.reviews :as review-hooks]
                 storefront.frontend-trackings])
            api.orders
            [clojure.string :refer [join]]
            [mayvenn.concept.booking :as booking]
            [mayvenn.concept.questioning :as questioning]
            [mayvenn.visual.tools :refer [with within]]
            [spice.selector :as selector]
            [storefront.effects :as fx]
            [storefront.events :as e]
            [storefront.keypaths :as k]
            [storefront.platform.messages
             :as messages
             :refer [handle-message]
             :rename {handle-message publish}]
            [storefront.trackings :as trk]
            [storefront.transitions :as t]))

#_
(def product-annotations
  {"120" {:customer/persona :customer.persona/p1}
   "236" {:customer/persona :customer.persona/p1}
   "9"   {:customer/persona :customer.persona/p1}
   "353" {:customer/persona :customer.persona/p1}
   "335" {:customer/persona :customer.persona/p2}
   "354" {:customer/persona :customer.persona/p2}
   "268" {:customer/persona :customer.persona/p2}
   "249" {:customer/persona :customer.persona/p2}
   "352" {:customer/persona :customer.persona/p3}
   #_#_"354" {:customer/persona :customer.persona/p3}
   "235" {:customer/persona :customer.persona/p3}
   "313" {:customer/persona :customer.persona/p3}
   #_#_"354" {:customer/persona :customer.persona/p4}
   "128" {:customer/persona :customer.persona/p4}
   "252" {:customer/persona :customer.persona/p4}
   "15" {:customer/persona :customer.persona/p4}})
#_
(def look-annotations
  {:7e4F9lGZHmTphADJxRFL6a {:customer/goals :customer.goals/enhance-natural}
   :2LcC986WiwwveAsh9HwEKZ {:customer/goals :customer.goals/protect-natural}
   :2KKdNG4k1JlFQWtOIXWHWB {:customer/goals :customer.goals/save-money}
   :74i7UgWjKVrAtV1mqMVMoh {:customer/goals :customer.goals/easy-maintenance}})

;;;; Models

(defn results
  [persona-id]
  (case persona-id
    :p1 [{:copy/title "Straight Center Part Lace Short Bob Wig", :catalog/product-id "120", :page/slug "timeless-toni-straight-center-part-lace-short-bob-wig", :catalog/sku-id "WIG-BOB-SCP-10-1B", :url "https://ucarecdn.com/dc278222-67a0-4b19-bcf8-cde86a80d8eb/"}
         {:copy/title "Brazilian Natural Straight Headband Wig", :catalog/product-id "236", :page/slug "brazilian-straight-headband-wigs", :catalog/sku-id "BNSHBW16", :url "https://ucarecdn.com/2f77c82b-1c29-45d0-b6ab-dfdc8999b3ba/"}
         {:title "Virgin Brazilian Straight 18\", 20\", 22\" + 18\" 4x4 HD Lace Closure", :content/id "5bg4Gcijd9AEI1lUp6PPOd", :photo-url "https://ucarecdn.com/6bac33f3-4a30-4b27-bf9c-6a04fa01e983/-/resize/550x/"}
         {:copy/title "Curly Top Lace Bob with Bangs Wig", :catalog/product-id "353", :page/slug "curly-top-lace-bob-bangs-wig", :catalog/sku-id "WIG-BOB-CTL-12-1B", :url "https://ucarecdn.com/d4275a4f-db01-441a-a3d5-68c183b09c44/"}]
    :p2 [{:copy/title "HD Lace Front Brazilian Yaki Straight Wig", :catalog/product-id "335", :page/slug "hd-lace-brazilian-yaki-straight-lace-front-wigs", :catalog/sku-id "BYSHDLFW16", :url "https://ucarecdn.com/55bd5c49-18aa-402f-b4c4-d008a3156dcb/"}
         {:copy/title "180g Straight Seamless Clip-Ins", :catalog/product-id "354", :page/slug "180g-straight-seamless-clip-ins", :catalog/sku-id "CLIP-S-B-4-6-20-180", :url "https://ucarecdn.com/333ac32a-0cb8-4405-a287-c8369365a66a/"}
         {:title "Virgin Malaysian Body Wave 18\", 20\", 20\" + 18\" 13x4 HD Lace Frontal", :content/id "4EUTW0z7cQzUPOoDSRQTFT", :photo-url "https://ucarecdn.com/403407c8-df3f-4103-9f16-c49f24635266/-/resize/550x/"}
         {:copy/title "HD Lace Front Brazilian Loose Wave Wig", :catalog/product-id "249", :page/slug "hd-lace-brazilian-loose-wave-lace-front-wigs", :catalog/sku-id "BLWHDLFW20", :url "https://ucarecdn.com/e83734ae-b4db-4d3c-a680-8cd1e6db923c/"}]
    :p3 [{:copy/title "Straight Center Part Long Bob 1B with Blonde Front Highlight Wig", :catalog/product-id "352", :page/slug "straight-center-part-long-bob-1b-blonde-front-highlight-wig", :catalog/sku-id "WIG-BOB-SCP-14-HL1B27", :url "https://ucarecdn.com/0598bb4c-3f1b-411c-8743-cfa17e51fd79/"}
         {:copy/title "180g Straight Seamless Clip-Ins", :catalog/product-id "354", :page/slug "180g-straight-seamless-clip-ins", :catalog/sku-id "CLIP-S-H-4-8-20-180", :url "https://ucarecdn.com/333ac32a-0cb8-4405-a287-c8369365a66a/"}
         {:copy/title "Standard Lace Front Indian Loose Wave Wig", :catalog/product-id "235", :page/slug "indian-loose-wave-lace-lace-front-wigs", :catalog/sku-id "ILWBLFW20", :url "https://ucarecdn.com/db9ca316-bb8d-4c3c-9b18-bbd5759156b1/"}
         {:content/id "1VxWKFdouTl7jRCzTktnLs" :title "Indian Straight 26\", 26\", 26\" + 18\" HD Lace Frontal " :photo-url "https://ucarecdn.com/48cbbc4a-9caf-4c54-a8fb-a16b4db932d8/-/scale_crop/1000x1000/smart/"}]
    :p4 [{:copy/title "180g Straight Seamless Clip-Ins", :catalog/product-id "354", :page/slug "180g-straight-seamless-clip-ins", :catalog/sku-id "CLIP-S-B-4-7-20-180", :url "https://ucarecdn.com/333ac32a-0cb8-4405-a287-c8369365a66a/"}
         {:copy/title "Straight Top Lace With Bangs Wig", :catalog/product-id "128", :page/slug "notorious-naomi-straight-top-lace-with-bangs-wig", :catalog/sku-id "WIG-STL-20-1B", :url "https://ucarecdn.com/3fb27662-3730-4170-a9a3-ec7c53e7fd24/"}
         {:copy/title "HD Lace Front Malaysian Body Wave Wig", :catalog/product-id "251", :page/slug "hd-lace-malaysian-body-wave-lace-front-wigs", :catalog/sku-id "MBWHDLFW16", :url "https://ucarecdn.com/1ff0e630-33ff-46da-987b-263c84d5392b/"}
         {:content/id "5s26Upsk2tEWYQFNaw1PBe" :title "Virgin Peruvian Body Wave 20\", 20\", 20\" Bundles " :photo-url "https://ucarecdn.com/e71bb365-9137-459a-9690-456548afa467/-/scale_crop/1000x1000/smart/"}]
    ;; default is p1
    [{:copy/title "Straight Center Part Lace Short Bob Wig", :catalog/product-id "120", :page/slug "timeless-toni-straight-center-part-lace-short-bob-wig", :catalog/sku-id "WIG-BOB-SCP-10-1B", :url "https://ucarecdn.com/dc278222-67a0-4b19-bcf8-cde86a80d8eb/"}
     {:copy/title "Brazilian Natural Straight Headband Wig", :catalog/product-id "236", :page/slug "brazilian-straight-headband-wigs", :catalog/sku-id "BNSHBW16", :url "https://ucarecdn.com/2f77c82b-1c29-45d0-b6ab-dfdc8999b3ba/"}
     {:title "Virgin Brazilian Straight 18\", 20\", 22\" + 18\" 4x4 HD Lace Closure", :content/id "5bg4Gcijd9AEI1lUp6PPOd", :photo-url "https://ucarecdn.com/6bac33f3-4a30-4b27-bf9c-6a04fa01e983/-/resize/550x/"}
     {:copy/title "Curly Top Lace Bob with Bangs Wig", :catalog/product-id "353", :page/slug "curly-top-lace-bob-bangs-wig", :catalog/sku-id "WIG-BOB-CTL-12-1B", :url "https://ucarecdn.com/d4275a4f-db01-441a-a3d5-68c183b09c44/"}]))

(defn <-
  "Get the results model of a look suggestion"
  [state]
  (when-let [persona-id (get-in state k/models-persona)]
    {:persona/id  persona-id
     :results     (results persona-id)}))

;;;; Behavior

;; Reset

(defmethod t/transition-state e/persona|reset
  [_ _ _ state]
  (-> state
      (assoc-in k/models-persona nil)))

;; Selected
;; Assume the event id or query the questionings for an answer

(defn ^:private calculate-persona-from-quiz
  [state]
  (let [{:keys [answers]} (questioning/<- state :crm/persona)]
    (case (:customer/styles answers)
      :customer.styles/everyday-look    :p1
      :customer.styles/work             :p2
      :customer.styles/switch-it-up     :p3
      :unsure                           :p3
      :customer.styles/special-occasion :p4
      :customer.styles/vacation         :p4
      :p3)))

(defmethod t/transition-state e/persona|selected
  [_ _ {:persona/keys [id]} state]
  (-> state
      (assoc-in k/models-persona (or id
                                     (calculate-persona-from-quiz state)))))

(defmethod fx/perform-effects e/persona|selected
  [_ _ {:keys [on/success-fn]} _ state]
  (when (fn? success-fn)
    (when-let [persona-id (name (get-in state k/models-persona))]
      (success-fn persona-id))))

#?(:cljs
   (defmethod trk/perform-track e/persona|selected
     [_ _ {:persona/keys [id]} state]
     (stringer/track-event "persona_assigned" {:persona id
                                               ;; Hardcoded for now
                                               :quiz_id "crm-persona"})))
