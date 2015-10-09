(ns me.maverickbozo.clj-jwplatform
  (:require [clj-time.coerce :refer [to-epoch]]
            [clj-time.core :refer [now]]
            [crypto.random :refer [url-part]]
            [cemerick.url :refer [url-encode url map->query]]
            [pandect.algo.sha1 :refer [sha1]]
            [clj-http.client :as hclient]
            [schema.core :as sch]
            [medley.core :refer [map-keys map-kv]]
            [clojure.string :as str]
            [camel-snake-kebab.core :refer [->snake_case]]))

(def JwpComponentSchema
  ""
  {:base-url sch/Str
   :api-key sch/Str
   :api-secret sch/Str})


(sch/defn ^:always-validate mk-jwp :- JwpComponentSchema
  "make jwp component"
  [api-key :- sch/Str api-secret :- sch/Str]
  {:base-url "https://api.jwplatform.com"
   :api-key api-key
   :api-secret api-secret})


(defn default-params
  ""
  [jwp]
  {"api_format" "json"
   "api_nonce"  (url-part 8)
   "api_timestamp" (-> (now) to-epoch int str)
   "api_key"    (:api-key jwp)})


(defn generate-signature [jwp qs]
  (-> (str (map->query qs) (:api-secret jwp))
      (sha1)))



(defn generate-signed-query-params
  ""
  [jwp ap]
  (assoc ap "api_signature" (generate-signature jwp ap)))


(defn generate-url [jwp path qs]
  (-> (url (:base-url jwp) path)
      (assoc :query qs)
      str))


(def NewVideoSchema
  "new video schema"
  {
   (sch/optional-key :title) sch/Str
   (sch/optional-key :tags) [sch/Str]
   (sch/optional-key :description) sch/Str
   (sch/optional-key :author) sch/Str
   (sch/optional-key :date) sch/Int
   (sch/optional-key :link) sch/Str
   (sch/optional-key :sourcetype) (sch/enum "file" "url")
   (sch/optional-key :sourceurl) sch/Str
   (sch/optional-key :sourceformat) (sch/enum "mp4" "webm" "flv" "aac" "mp3" "vorbis" "youtube")
   (sch/optional-key :download_url) sch/Str
   (sch/optional-key :custom-params) (sch/pred map?)
   (sch/optional-key :md5) sch/Str
   (sch/optional-key :resumable) (sch/enum "True"  "False")
   (sch/optional-key :size) sch/Int
   })


(def kw->str (comp str name))


(defn create-video-input->flatmap
  ""
  [inp]
  (let [cp (get inp :custom-params {})
        mcp (into {} (map (fn [[k v]] 
                            [(str "custom." (->snake_case ((comp str name) k)))
                             (->snake_case v)])
                          cp))
        inp* (-> (dissoc inp :custom-params)
                 (merge mcp))]
    (map-kv (fn [k v]
              (case k
                :tags [k (str/join "," v)]
                [k v])) inp*)
    )
  )



(sch/defn ^:always-validate create-video
  ""
  [jwp :- JwpComponentSchema inp :- NewVideoSchema]
  (let [ap (merge (default-params jwp) (map-keys (comp str name) (create-video-input->flatmap inp)))
        sap (generate-signed-query-params jwp ap)
        url (generate-url jwp "/videos/create" sap)]
    url))
