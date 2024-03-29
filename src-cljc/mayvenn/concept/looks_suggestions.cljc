(ns mayvenn.concept.looks-suggestions
  "
  Concept: Looks Suggestions

  Joining Questioning and Looks, awkwardly.

  Discussion

  Looks are poorly modeled in our system, and this conception joins
  Questioning and Looks, but doesn't cleanly separate Looks.

  If looks were defined so they could be selected, the mini-cellar
  and bespoke logic below might not be necessary.
  "
  (:require #?@(:cljs
                [[storefront.hooks.stringer :as stringer]
                 [storefront.api :as api]
                 [storefront.hooks.reviews :as review-hooks]
                 storefront.frontend-trackings])
            api.orders
            [mayvenn.concept.booking :as booking]
            [mayvenn.concept.questioning :as questioning]
            [clojure.string :refer [join]]
            [storefront.events :as e]
            [storefront.effects :as fx]
            [storefront.keypaths :as k]
            [storefront.trackings :as trk]
            [storefront.transitions :as t]
            [storefront.accessors.experiments :as experiments]
            [storefront.platform.messages
             :as messages
             :refer [handle-message]
             :rename {handle-message publish}]))

(def BNS-short {:img/id             "16029dd0-285c-4bc6-803c-0c201c3d402c"
                :img.v2/id          "ec4c6d2c-7bbe-4ecc-8d7c-e9d38f62cf70"
                :hair/origin        "Brazilian"
                :hair/texture       "Straight"
                :service/sku-id     "SV2-LBI-X"
                :contentful/look-id "1OI4u29AlFOkW4Gf86KPFo"
                :product/sku-ids    ["BNS10","BNS12","BNS14"]})

(def BNS-short-closure
  (-> BNS-short
      (update :product/sku-ids conj "BNSLC10")
      (assoc :service/sku-id "SV2-CBI-X")
      (assoc :contentful/look-id "2tZXpzpXVJfsRLjMG5S6ZZk")
      (assoc :img.v2/id "6c6f87fd-82b1-4de4-9c70-9a9b88233a56")))

(def BNS-medium {:img/id             "16029dd0-285c-4bc6-803c-0c201c3d402c"
                 :img.v2/id          "f3b05633-f844-4878-96c8-4ebc21eec7d3"
                 :hair/origin        "Brazilian"
                 :hair/texture       "Straight"
                 :service/sku-id     "SV2-LBI-X"
                 :contentful/look-id "52RML7ed0csprm0rOah5oo"
                 :product/sku-ids    ["BNS14","BNS16","BNS18"]})

(def BNS-medium-closure
  (-> BNS-medium
      (update :product/sku-ids conj "BNSLC14")
      (assoc :service/sku-id "SV2-CBI-X")
      (assoc :contentful/look-id "2k2VWRwb85ELRIdgebABn9")
      (assoc :img.v2/id "10bb8fad-24c5-42db-bb08-14d96aa079e7")))

(def BNS-long {:img/id             "16029dd0-285c-4bc6-803c-0c201c3d402c"
               :img.v2/id          "3046e95c-b84f-4089-aff1-5281bf90f34a"
               :hair/origin        "Brazilian"
               :hair/texture       "Straight"
               :service/sku-id     "SV2-LBI-X"
               :contentful/look-id "3rYucFFsWuXOq5esnEv8jz"
               :product/sku-ids    ["BNS18","BNS20","BNS22"]})

(def BNS-long-closure
  (-> BNS-long
      (assoc :service/sku-id "SV2-CBI-X")
      (update :product/sku-ids conj "BNSLC18")
      (assoc :contentful/look-id "6KCszWgp1Amqk6RINt1yJG")
      (assoc :img.v2/id "9281dff6-5f2c-4b8d-bbe2-ffe8108411e1")))

(def BNS-extra-long {:img/id             "16029dd0-285c-4bc6-803c-0c201c3d402c"
                     :img.v2/id          "3c3efe6e-52f5-445c-b10c-efb12c34b844"
                     :hair/origin        "Brazilian"
                     :hair/texture       "Straight"
                     :service/sku-id     "SV2-LBI-X"
                     :contentful/look-id "17Yq5Wot0J25sjynj8YlyT"
                     :product/sku-ids    ["BNS22","BNS24","BNS26"]})

(def BNS-extra-long-closure
  (-> BNS-extra-long
      (assoc :service/sku-id "SV2-CBI-X")
      (assoc :contentful/look-id "24S3V2pwKt8eYfG2inyXzp")
      (update :product/sku-ids conj "BNSLC18")
      (assoc :img.v2/id  "4fed4a71-25b4-4068-bb3c-c756cccc0e7b")))

(def BLW-short {:img/id             "f7568a58-d240-4856-9d7d-21096bafda1c"
                :img.v2/id          "dca63c93-6395-4243-b803-e926219bf6cc"
                :hair/origin        "Brazilian"
                :hair/texture       "Loose Wave"
                :service/sku-id     "SV2-LBI-X"
                :contentful/look-id "52LaTXlyH7LXbPUR2lkMe5"
                :product/sku-ids    ["BLW10","BLW12","BLW14"]})

(def BLW-short-closure
  (-> BLW-short
      (assoc :service/sku-id "SV2-CBI-X")
      (assoc :contentful/look-id "3HzQFq94Wy9eiceDWqylFZ")
      (update :product/sku-ids conj "BLWLC10")
      (assoc :img.v2/id "532ed776-3e77-441f-9eda-97f681ed61ce")))

(def BLW-medium {:img/id             "f7568a58-d240-4856-9d7d-21096bafda1c"
                 :img.v2/id          "7e0b869b-e581-4f25-afa7-5a41822c69bb"
                 :hair/origin        "Brazilian"
                 :hair/texture       "Loose Wave"
                 :service/sku-id     "SV2-LBI-X"
                 :contentful/look-id "5LSNb6farZBU3i7woLGNQw"
                 :product/sku-ids    ["BLW14","BLW16","BLW18"]})

(def BLW-medium-closure
  (-> BLW-medium
      (assoc :img.v2/id "f8a5a7ed-8bc4-4070-b6ce-165d6bdbcd84")
      (assoc :service/sku-id "SV2-CBI-X")
      (assoc :contentful/look-id "54CVX3cSx2uMSAI7cotYFC")
      (update :product/sku-ids conj "BLWLC14")))

(def BLW-long {:img/id             "f7568a58-d240-4856-9d7d-21096bafda1c"
               :img.v2/id          "0ca0d1c3-7890-4a7d-8196-4bfe1c146052"
               :hair/origin        "Brazilian"
               :hair/texture       "Loose Wave"
               :service/sku-id     "SV2-LBI-X"
               :contentful/look-id "1uQ3s5yo7qp1gH6AmfwWie"
               :product/sku-ids    ["BLW18","BLW20","BLW22"]})

(def BLW-long-closure
  (-> BLW-long
      (assoc :img.v2/id "7f3bc76d-dd5f-4c6d-85da-78a464053e42")
      (assoc :service/sku-id "SV2-CBI-X")
      (assoc :contentful/look-id "2OgSTEcjHgXLrSZKAUTCEM")
      (update :product/sku-ids conj "BLWLC18")))

(def BLW-extra-long
  {:img/id             "f7568a58-d240-4856-9d7d-21096bafda1c"
   :img.v2/id          "ca057c39-8f8e-40d0-b56a-cb61da778433"
   :hair/origin        "Brazilian"
   :hair/texture       "Loose Wave"
   :service/sku-id     "SV2-LBI-X"
   :contentful/look-id "6gMgpJPpLuIJVxI1mKwvor"
   :product/sku-ids    ["BLW22","BLW24","BLW26"]})

(def BLW-extra-long-closure
  (-> BLW-extra-long
      (assoc :img.v2/id "13ab394f-50a6-42c4-bd42-c6eac4b214af")
      (assoc :service/sku-id "SV2-CBI-X")
      (assoc :contentful/look-id "3Zif5aQOJODMhMbmtPXf43")
      (update :product/sku-ids conj "BLWLC18")))

(def MBW-short {:img/id             "888b9c79-265d-4547-b8ce-0c7ce56c8741"
                :img.v2/id          "56ddd200-987c-40aa-88d7-0fee810531bf"
                :hair/origin        "Malaysian"
                :hair/texture       "Body Wave"
                :service/sku-id     "SV2-LBI-X"
                :contentful/look-id "2Zwn9ANekMkrK5Bkvshr6P"
                :product/sku-ids    ["MBW10","MBW12","MBW14"]})

(def MBW-short-closure
  (-> MBW-short
      (assoc :service/sku-id "SV2-CBI-X")
      (assoc :contentful/look-id "6almUjTNHSKtRWn1qxCAtU")
      (update :product/sku-ids conj "MBWLC10")
      (assoc :img.v2/id "ea30a27f-073f-43d3-a2e0-3e4c54ce9c42")))

(def MBW-medium {:img/id             "888b9c79-265d-4547-b8ce-0c7ce56c8741"
                 :img.v2/id          "3e827c3e-4188-4a87-ba53-7fe2eb4df7db"
                 :hair/origin        "Malaysian"
                 :hair/texture       "Body Wave"
                 :service/sku-id     "SV2-LBI-X"
                 :contentful/look-id "6dlpwvbjIs33qRtTyNHrng"
                 :product/sku-ids    ["MBW14","MBW16","MBW18"]})

(def MBW-medium-closure
  (-> MBW-medium
      (assoc :img.v2/id "d0320ddd-d5cf-4cea-88c7-0e1697aafc03")
      (assoc :service/sku-id "SV2-CBI-X")
      (assoc :contentful/look-id "7qXP6tQEPoDbLnDmpudQx2")
      (update :product/sku-ids conj "MBWLC14")))

(def MBW-long {:img/id             "888b9c79-265d-4547-b8ce-0c7ce56c8741"
               :img.v2/id          "1ff91ee2-4185-40b6-991c-f373602dcdfa"
               :hair/origin        "Malaysian"
               :hair/texture       "Body Wave"
               :service/sku-id     "SV2-LBI-X"
               :contentful/look-id "1EGgA8ObvrgVRtk9haVbXf"
               :product/sku-ids    ["MBW18","MBW20","MBW22"]})

(def MBW-long-closure
  (-> MBW-long
      (assoc :img.v2/id "29ad2f3b-6619-45b0-beb7-7f403313f9fb")
      (assoc :service/sku-id "SV2-CBI-X")
      (assoc :contentful/look-id "6Po219PYckRjbstnEVzQb6")
      (update :product/sku-ids conj "MBWLC18")))

(def MBW-extra-long {:img/id             "888b9c79-265d-4547-b8ce-0c7ce56c8741"
                     :img.v2/id          "d95e6b87-0a32-4879-ab87-7f59f0303e2d"
                     :hair/origin        "Malaysian"
                     :hair/texture       "Body Wave"
                     :service/sku-id     "SV2-LBI-X"
                     :contentful/look-id "6VF9FqYPpa208FEmgH26FN"
                     :product/sku-ids    ["MBW22","MBW24","MBW26"]})

(def MBW-extra-long-closure
  (-> MBW-extra-long
      (assoc :img.v2/id "b5bca431-4884-4753-bd79-666befb323af")
      (assoc :service/sku-id "SV2-CBI-X")
      (assoc :contentful/look-id "5NURIEnq53fs3038U3ir20")
      (update :product/sku-ids conj "MBWLC18")))

(def mini-cellar
  {"BLW10"
   {:hair/family "bundles",
    :sku/price 59.99,
    :hair/length "10",
    :catalog/sku-id "BLW10",
    :legacy/variant-id 599},
   "BLW12"
   {:hair/family "bundles",
    :sku/price 64.99,
    :hair/length "12",
    :catalog/sku-id "BLW12",
    :legacy/variant-id 2},
   "BLW14"
   {:hair/family "bundles",
    :sku/price 69.99,
    :hair/length "14",
    :catalog/sku-id "BLW14",
    :legacy/variant-id 3},
   "BLW16"
   {:hair/family "bundles",
    :sku/price 74.99,
    :hair/length "16",
    :catalog/sku-id "BLW16",
    :legacy/variant-id 4},
   "BLW18"
   {:hair/family "bundles",
    :sku/price 79.99,
    :hair/length "18",
    :catalog/sku-id "BLW18",
    :legacy/variant-id 5},
   "BLW20"
   {:hair/family "bundles",
    :sku/price 84.99,
    :hair/length "20",
    :catalog/sku-id "BLW20",
    :legacy/variant-id 6},
   "BLW22"
   {:hair/family "bundles",
    :sku/price 89.99,
    :hair/length "22",
    :catalog/sku-id "BLW22",
    :legacy/variant-id 7},
   "BLW24"
   {:hair/family "bundles",
    :sku/price 114.99,
    :hair/length "24",
    :catalog/sku-id "BLW24",
    :legacy/variant-id 8},
   "BLW26"
   {:hair/family "bundles",
    :sku/price 134.99,
    :hair/length "26",
    :catalog/sku-id "BLW26",
    :legacy/variant-id 9},
   "BLWLC10"
   {:hair/family "closures",
    :sku/price 89.99,
    :hair/length "10",
    :catalog/sku-id "BLWLC10",
    :legacy/variant-id 600},
   "BLWLC14"
   {:hair/family "closures",
    :sku/price 94.99,
    :hair/length "14",
    :catalog/sku-id "BLWLC14",
    :legacy/variant-id 12},
   "BLWLC18"
   {:hair/family "closures",
    :sku/price 104.99,
    :hair/length "18",
    :catalog/sku-id "BLWLC18",
    :legacy/variant-id 13},
   "BNS10"
   {:hair/family "bundles",
    :sku/price 55.99,
    :hair/length "10",
    :img/url "//ucarecdn.com//",
    :catalog/sku-id "BNS10",
    :legacy/variant-id 479},
   "BNS12"
   {:hair/family "bundles",
    :sku/price 59.99,
    :hair/length "12",
    :catalog/sku-id "BNS12",
    :legacy/variant-id 80},
   "BNS14"
   {:hair/family "bundles",
    :sku/price 64.99,
    :hair/length "14",
    :catalog/sku-id "BNS14",
    :legacy/variant-id 81},
   "BNS16"
   {:hair/family "bundles",
    :sku/price 69.99,
    :hair/length "16",
    :catalog/sku-id "BNS16",
    :legacy/variant-id 82},
   "BNS18"
   {:hair/family "bundles",
    :sku/price 74.99,
    :hair/length "18",
    :catalog/sku-id "BNS18",
    :legacy/variant-id 83},
   "BNS20"
   {:hair/family "bundles",
    :sku/price 79.99,
    :hair/length "20",
    :catalog/sku-id "BNS20",
    :legacy/variant-id 84},
   "BNS22"
   {:hair/family "bundles",
    :sku/price 84.99,
    :hair/length "22",
    :catalog/sku-id "BNS22",
    :legacy/variant-id 85},
   "BNS24"
   {:hair/family "bundles",
    :sku/price 109.99,
    :hair/length "24",
    :catalog/sku-id "BNS24",
    :legacy/variant-id 86},
   "BNS26"
   {:hair/family "bundles",
    :sku/price 129.99,
    :hair/length "26",
    :catalog/sku-id "BNS26",
    :legacy/variant-id 87},
   "BNSLC10"
   {:hair/family "closures",
    :sku/price 89.99,
    :hair/length "10",
    :catalog/sku-id "BNSLC10",
    :legacy/variant-id 601},
   "BNSLC14"
   {:hair/family "closures",
    :sku/price 94.99,
    :hair/length "14",
    :catalog/sku-id "BNSLC14",
    :legacy/variant-id 90},
   "BNSLC18"
   {:hair/family "closures",
    :sku/price 104.99,
    :hair/length "18",
    :catalog/sku-id "BNSLC18",
    :legacy/variant-id 91},
   "MBW10"
   {:hair/family "bundles",
    :sku/price 64.99,
    :hair/length "10",
    :img/url "//ucarecdn.com//",
    :catalog/sku-id "MBW10",
    :legacy/variant-id 602},
   "MBW12"
   {:hair/family "bundles",
    :sku/price 69.99,
    :hair/length "12",
    :catalog/sku-id "MBW12",
    :legacy/variant-id 15},
   "MBW14"
   {:hair/family "bundles",
    :sku/price 74.99,
    :hair/length "14",
    :catalog/sku-id "MBW14",
    :legacy/variant-id 16},
   "MBW16"
   {:hair/family "bundles",
    :sku/price 79.99,
    :hair/length "16",
    :catalog/sku-id "MBW16",
    :legacy/variant-id 17},
   "MBW18"
   {:hair/family "bundles",
    :sku/price 84.99,
    :hair/length "18",
    :catalog/sku-id "MBW18",
    :legacy/variant-id 18},
   "MBW20"
   {:hair/family "bundles",
    :sku/price 89.99,
    :hair/length "20",
    :catalog/sku-id "MBW20",
    :legacy/variant-id 19},
   "MBW22"
   {:hair/family "bundles",
    :sku/price 94.99,
    :hair/length "22",
    :catalog/sku-id "MBW22",
    :legacy/variant-id 20},
   "MBW24"
   {:hair/family "bundles",
    :sku/price 119.99,
    :hair/length "24",
    :catalog/sku-id "MBW24",
    :legacy/variant-id 21},
   "MBW26"
   {:hair/family "bundles",
    :sku/price 139.99,
    :hair/length "26",
    :catalog/sku-id "MBW26",
    :legacy/variant-id 22},
   "MBWLC10"
   {:hair/family "closures",
    :sku/price 89.99,
    :hair/length "10",
    :catalog/sku-id "MBWLC10",
    :legacy/variant-id 603},
   "MBWLC14"
   {:hair/family "closures",
    :sku/price 94.99,
    :hair/length "14",
    :catalog/sku-id "MBWLC14",
    :legacy/variant-id 25},
   "MBWLC18"
   {:hair/family "closures",
    :sku/price 104.99,
    :hair/length "18",
    :catalog/sku-id "MBWLC18",
    :legacy/variant-id 26}
   "SV2-LBI-X"
   {:hair/family "bundles",
    :sku/price 0, ;; Free in this context
    :catalog/sku-id "SV2-LBI-X",
    :legacy/variant-id 1156}
   "SV2-CBI-X"
   {:hair/family "closures",
    :sku/price 0, ;; Free in this context
    :catalog/sku-id "SV2-CBI-X",
    :legacy/variant-id 1114}})

(def answers->results
  {:straight   {:short      {:yes    [BNS-short              BNS-medium]
                             :no     [BNS-short-closure      BNS-medium-closure]
                             :unsure [BNS-short              BNS-short-closure]}
                :medium     {:yes    [BNS-medium             BNS-long]
                             :no     [BNS-medium-closure     BNS-long-closure]
                             :unsure [BNS-medium             BNS-medium-closure]}
                :long       {:yes    [BNS-long               BNS-extra-long]
                             :no     [BNS-long-closure       BNS-extra-long-closure]
                             :unsure [BNS-long               BNS-long-closure]}
                :extra-long {:yes    [BNS-extra-long         BNS-long]
                             :no     [BNS-extra-long-closure BNS-long-closure]
                             :unsure [BNS-extra-long         BNS-extra-long-closure]}
                :unsure     {:yes    [BNS-medium             BNS-long]
                             :no     [BNS-medium-closure     BNS-long-closure]
                             :unsure [BNS-medium             BNS-long-closure]}}
   :loose-wave {:short      {:yes    [BLW-short              BLW-medium]
                             :no     [BLW-short-closure      BLW-medium-closure]
                             :unsure [BLW-short              BLW-short-closure]}
                :medium     {:yes    [BLW-medium             BLW-long]
                             :no     [BLW-medium-closure     BLW-long-closure]
                             :unsure [BLW-medium             BLW-medium-closure]}
                :long       {:yes    [BLW-long               BLW-extra-long]
                             :no     [BLW-long-closure       BLW-extra-long-closure]
                             :unsure [BLW-long               BLW-long-closure]}
                :extra-long {:yes    [BLW-extra-long         BLW-long]
                             :no     [BLW-extra-long-closure BLW-long-closure]
                             :unsure [BLW-extra-long         BLW-extra-long-closure]}
                :unsure     {:yes    [BLW-medium             BLW-long]
                             :no     [BLW-medium-closure     BLW-long-closure]
                             :unsure [BLW-medium             BLW-long-closure]}}
   :body-wave  {:short      {:yes    [MBW-short              MBW-medium]
                             :no     [MBW-short-closure      MBW-medium-closure]
                             :unsure [MBW-short              MBW-short-closure]}
                :medium     {:yes    [MBW-medium             MBW-long]
                             :no     [MBW-medium-closure     MBW-long-closure]
                             :unsure [MBW-medium             MBW-medium-closure]}
                :long       {:yes    [MBW-long               MBW-extra-long]
                             :no     [MBW-long-closure       MBW-extra-long-closure]
                             :unsure [MBW-long               MBW-long-closure]}
                :extra-long {:yes    [MBW-extra-long         MBW-long]
                             :no     [MBW-extra-long-closure MBW-long-closure]
                             :unsure [MBW-extra-long         MBW-extra-long-closure]}
                :unsure     {:yes    [MBW-medium             MBW-long]
                             :no     [MBW-medium-closure     MBW-long-closure]
                             :unsure [MBW-medium             MBW-long-closure]}}
   :unsure     {:short      {:yes    [BNS-short              BLW-short]
                             :no     [BNS-short-closure      BLW-short-closure]
                             :unsure [BNS-short              BLW-short-closure]}
                :medium     {:yes    [BNS-medium             BLW-medium]
                             :no     [BNS-medium-closure     BLW-medium-closure]
                             :unsure [BNS-medium             BLW-medium-closure]}
                :long       {:yes    [BNS-long               BLW-long]
                             :no     [BNS-long-closure       BLW-long-closure]
                             :unsure [BNS-long               BLW-long-closure]}
                :extra-long {:yes    [BNS-extra-long         BLW-extra-long]
                             :no     [BNS-extra-long-closure BLW-extra-long-closure]
                             :unsure [BNS-extra-long         BLW-extra-long-closure]}
                :unsure     {:yes    [BNS-medium             BLW-long]
                             :no     [BNS-medium-closure     BLW-long-closure]
                             :unsure [BNS-medium             BLW-long-closure]}}})

(defn <-
  [state id]
  (->> id
       (conj k/models-looks-suggestions)
       (get-in state)))

(defn selected<-
  "
  A selected look based on the conception of looks in this namespace.
  We don't have a canonical schema or ids for these yet.

  purpose-id: the id to store a selected at, determined by caller
  "
  [state purpose-id]
  (->> purpose-id
       (conj k/models-look-selected)
       (get-in state)))

;;;; Behavior

;; Reset

(defmethod t/transition-state e/biz|looks-suggestions|reset
  [_ _ {:keys [id]} state]
  (-> state
      (assoc-in (conj k/models-looks-suggestions id) nil)
      (assoc-in (conj k/models-look-selected id) nil)))

;; Queried

(defmethod fx/perform-effects e/biz|looks-suggestions|queried
  [_ _ {:questioning/keys [id] answers :answers} _ _]
  (publish e/flow|wait|begun
           {:wait/id id})
  (let [{:keys [texture length leave-out]} answers]
    (publish e/biz|looks-suggestions|resulted
             {:id      id
              :results (->> [texture length leave-out]
                            (get-in answers->results))})))

;; Resulted

(defmethod t/transition-state e/biz|looks-suggestions|resulted
  [_ _ {:keys [id results]} state]
  (assoc-in state
            (conj k/models-looks-suggestions id)
            results))

(defmethod fx/perform-effects e/biz|looks-suggestions|resulted
  [_ _ {:keys [id results]} _]
  #?(:cljs (review-hooks/start))
  (publish e/ensure-sku-ids
           {:sku-ids (set
                      (concat
                       (mapcat :product/sku-ids results)
                       (map :service/sku-id results)))}))

(defmethod trk/perform-track e/biz|looks-suggestions|resulted
  [_ _ {:keys [id results]} state]
  (let [{:keys [questions answers]} (questioning/<- state id)]
    (->> {:all_question_copy (->> questions
                                  (mapv (comp (partial join " ")
                                              :question/prompt)))
          :all_answers_copy  (->> questions
                                  (mapv (fn [{:question/keys [id choices]}]
                                          (let [choice-id (get answers id)]
                                            (->> choices
                                                 (filter (comp #{choice-id}
                                                               :choice/id))
                                                 first
                                                 :choice/answer)))))
          :quiz_results      (->> results
                                  (mapv
                                   (fn [{product-sku-ids :product/sku-ids
                                         service-sku-id  :service/sku-id}]
                                     (->> (conj product-sku-ids service-sku-id)
                                          (mapv (fn [sku-id]
                                                  (-> (get mini-cellar sku-id)
                                                      (select-keys [:legacy/variant-id :catalog/sku-id]))))))))}
         #?(:cljs (stringer/track-event "quiz_submitted")))))

(defmethod t/transition-state e/biz|looks-suggestions|confirmed
  [_ _ {:keys [id selected-look]} state]
  (assoc-in state
            (conj k/models-look-selected id)
            selected-look))

(defmethod fx/perform-effects e/biz|looks-suggestions|selected
  [_ _ {:keys [selected-look id] success :on/success} _ state]
  (let [{product-sku-ids :product/sku-ids
         service-sku-id  :service/sku-id}                   selected-look
        {servicing-stylist-id :services/stylist-id}         (api.orders/services state (get-in state k/order))
        {::booking/keys [selected-time-slot selected-date]} (booking/<- state)]
    #?(:cljs
       (api/new-order-from-sku-ids (get-in state k/session-id)
                                   {:store-stylist-id     (get-in state k/store-stylist-id)
                                    :servicing-stylist-id servicing-stylist-id
                                    :user-id              (get-in state k/user-id)
                                    :user-token           (get-in state k/user-token)
                                    :sku-id->quantity     (->> (conj product-sku-ids service-sku-id)
                                                               (map (fn [s] [s 1]))
                                                               (into {}))}
                                   ;; TODO: more specific handler
                                   (fn [{:keys [order]}]
                                     (messages/handle-message e/biz|looks-suggestions|confirmed
                                                              {:id id
                                                               :selected-look selected-look})
                                     (messages/handle-message
                                      e/api-success-update-order
                                      (merge
                                       {:order order}
                                       (if success
                                         {:on/success success}
                                         {:navigate e/navigate-cart})))
                                     (when (and selected-time-slot selected-date)
                                       (api/set-appointment-time-slot {:slot-id selected-time-slot
                                                                       :date    selected-date
                                                                       :number  (:number order)
                                                                       :token   (:token order)}))
                                     (storefront.frontend-trackings/track-cart-initialization
                                      "shopping-quiz"
                                      nil
                                      {:skus-db          (get-in state k/v2-skus)
                                       :image-catalog    (get-in state k/v2-images)
                                       :store-experience (get-in state k/store-experience)
                                       :order            order
                                       :user-ecd         (get-in state k/user-ecd)}))))))
