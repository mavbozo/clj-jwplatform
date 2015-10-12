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


;; bump version

 
(def JwpComponentSchema
  "this library component."
  {:base-url sch/Str
   :api-key sch/Str
   :api-secret sch/Str})


(sch/defn ^:always-validate mk-jwp :- JwpComponentSchema
  "make jwp component"
  [api-key :- sch/Str api-secret :- sch/Str]
  {:base-url "https://api.jwplatform.com"
   :api-key api-key
   :api-secret api-secret})


(defn- default-jwp-params
  [jwp]
  {"api_format" "json"
   "api_nonce"  (url-part 8)
   "api_timestamp" (-> (now) to-epoch int str)
   "api_key"    (:api-key jwp)})


(defn- jwp-signature
  "create jwp-signature for jwp parameters params"
  [jwp params]
  (-> (str (map->query params) (:api-secret jwp))
      (sha1)))



(defn- signed-params
  [jwp params]
  (assoc params "api_signature" (jwp-signature jwp params)))



(def NewVideoSchema
  "new video schema"
  {(sch/optional-key :title) sch/Str
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
   (sch/optional-key :size) sch/Int})


(defn- kw->str
  [k]
  ((comp str name) k))


(defn- cv-input-param->jwp-param
  "convert create-video's input parameter to jwp parameter map"
  [inp]
  (let [cp (get inp :custom-params {})
        mcp (into {} (map (fn [[k v]] 
                            [(str "custom." (->snake_case (kw->str k)))
                             (->snake_case v)])
                          cp))
        inp* (-> (dissoc inp :custom-params)
                 (merge mcp))]
    (map-kv (fn [k v]
              (case k
                :tags [k (str/join "," v)]
                [k v])) inp*)))


(defn- jwp-api-url 
  "return jw platform api url based on path and signed query params string"
  [jwp path params]
  (let [params* (merge (default-jwp-params jwp) params)
        signed-params (signed-params jwp params*)]
    (-> (url (:base-url jwp) path)
        (assoc :query signed-params)
        str)))

(defn- create-video-url
  [jwp inp]
  (let [action-path "/videos/create"
        cv-params (map-keys kw->str (cv-input-param->jwp-param inp))]
    (jwp-api-url jwp action-path cv-params)))


(sch/defn ^:always-validate create-video
  "jwplatform api: create video" 
  [jwp :- JwpComponentSchema inp :- NewVideoSchema]
  (try
    (-> (hclient/get (create-video-url jwp inp) {:as :json :coerce :always})
        :body)
    (catch Exception e
      (-> e ex-data :body)
      #_(throw (ex-info "clj-jwplatform api call error" (-> e ex-data :body))))))

