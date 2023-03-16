(ns mayvenn.concept.awareness
  "This is probably awareness")

(def hdyhau
  {:social-meta     "Facebook / Instagram"
   :friends-family  "Friends / Family"
   :news            "News Article"
   :tiktok          "TikTok"
   :social-other    "Other Social (YouTube, Snapchat, etc)"
   :search-engine   "Search Engine (Google, Bing)"
   :stylist         "Stylist"
   :billboard       "Billboard / Poster"
   :saw-store       "Saw the store/Walk-in"
   :other           "Other"
   :streaming-radio "Pandora, Spotify, etc"
   :mirror-cling    "Restroom Mirror Cling"
   :scratch-off     "Scratch-Off Ticket"
   :valpak-mailer   "ValPak Mailer"})

(defn hdyhau-answered-data
  [hdyhau-to-submit ; map {key true}
   hdyhau-options ;list of keys
   user-data]
  (merge
   {:hdyhau (map (fn [option]
                   {option (or (option hdyhau-to-submit) false)})
                  hdyhau-options)}
   user-data))
