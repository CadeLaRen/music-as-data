;; (play (pattern [A B C [D E]]))
;; (play (bmp 10 (pattern [A B C [D E]])))
;; (play (+ 10 (pattern [A B C [D E]])))
;; (play (reverse (pattern [A B C [D E]])))
;; (play (pattern [sin])))
;; (play (pattern [kick crush kick crush])))
;; (play (pattern [(+ kick crush) boot (+ kick crush)]))
;; (def sigs (play (pattern [A B])))
;; (stop sigs)

;; play should execute the timings

(ns examples.music
  (:import [ddf.minim Minim AudioOutput AudioSample]
		   [ddf.minim.signals SquareWave]
		   [ddf.minim.signals SineWave]
		   [ddf.minim.effects LowPassFS])
  (:import (processing.core PApplet PImage PGraphics PFont))
  (:use [rosado.processing]
        [rosado.processing.applet]))


(def *minim* (atom nil))
(def *outp* (atom nil))
(def *samples-path* "/home/darksun4/Sources/clj-processing/examples/data/")
(def *pattern* (atom []))


(defn play-note [note]
  (.playNote @*outp* (float 0)
			 (float (:duration note))
			 (:data note)))


(defn play-sample [sample]
  (.trigger (:data sample)))


(defrecord Melement [volume pitch duration play-fn data])

(defmacro defsample [sample sample-path]
  `(def ~sample
		(Melement. 50 (float 1.0) (float 1.0) play-sample
				   (.loadSample @*minim* (str *samples-path* '~sample-path)))))

(defmacro defnote [n-name]
  `(def ~n-name (Melement. 50 (float 1.0) (float 1.0) play-note (str '~n-name))))


(defn play-element [element]
  ((:play-fn element) element))


(defn- play-chord
  "Executes the play-funcs of of all elements"
  [chord-elements]
  (doseq [element (:data chord-elements)]
	(play-element element)))


(defn join [& notes]
  (Melement. 0 0 0 play-chord
			 (first (conj [] notes))))

;; Maybe defmulti?
(defmacro + [& more]
  ;; make chords like (+ kick hihat)
  `(join ~@more))

;; Notes
(defnote A)
;;(defnote B)
;;(defnote C)
;;(defnote D)
;;(defnote E)
;;(defnote F)
;;(defnote G)
;;(defnote E3)
;;(defnote G3)
;;(defnote B3)
;;(defnote E4)
;;(defnote _)
(def _ (Melement. 50 1.0 nil (fn[x] nil) "_"))


;; Nothing else matters
;;(p (pattern [E3 G3 B3 E4 B3 G3], 4))

;; Working example
;; (p (pattern [kick (+ hihat snare) kick],2))
(defn show-properties [prop pattern]
  (map #(prop %)
	   pattern))


;; Maybe a reduce could clean it up more?
(defn pattern
  ([m-element] (pattern m-element 1))
  ([m-element duration]
	 (if (= (type []) (type m-element))
	   (flatten
		(calc-duration m-element duration (count m-element)))
		(assoc m-element :duration duration))))


(defn calc-duration [elements duration count]
  (map #(pattern % (/ duration count))
	   elements))

(defn p
  [elements]
  (doseq [element elements]
	(play-element element)
	(Thread/sleep (* (:duration element) 500))))


(defn bpm [pattern tempo-num]
  (map #(+ (:duration %)
		   tempo-num)
	   pattern))

(defn setup []
  "Runs once."
  (swap! *minim* (fn [minim] (Minim. *applet*)))
  (swap! *outp* (fn [out]
				  (.getLineOut @*minim*))))

(defn draw []
  (background-float 124))

(defapplet example :title "Music as Data"
  :setup setup :draw draw :size [200 200])

(run example)
(stop example)

;; Drums
(defsample kick "KickDrums1/kickdrum6.wav")
(defsample kick2 "KickDrums4/kickdrum154.wav")
(defsample snare "SnareDrums1/snaredrum2.wav")
(defsample snare2 "DistortedSnares2/distortedsnare52.wav")
(defsample hihat "HiHats1/hihat2.wav")

(defmacro times [num expression]
  ;; small tempo drums demo
  `(doseq [i# (range ~num)]
	~expression))

(defn keep-looping []
  (times 500
		 (p (pattern @*pattern*))))

(keep-looping)

;; tron
;;http://www.newgrounds.com/audio/listen/379726
(p (pattern [kick kick kick kick snare kick kick kick], 3))
(times 8
	   (p (pattern [kick2 kick2 kick2 kick2 snare2 kick2 kick2 kick2], 2)))

(defn play! [new-pattern]
  (swap! *pattern* (fn[x] new-pattern)))

;; Drum Demos
(times 8 
  (p (pattern [kick (+ hihat snare)],2)))
(times 4
	   (p (pattern [kick (+ hihat snare) [kick kick] (+ hihat snare)],3)))
(times 4
	   (p (pattern [kick (+ hihat snare) [kick kick kick kick] kick kick kick kick (+ hihat snare)],3)))


(defn play-signal [signal]
  (.setFreq (:data signal) (:pitch signal))  
  (.addSignal @*outp*
			  (:data signal))
  (Thread/sleep (* (:duration signal) 500))	
  (.removeSignal @*outp*
				 (:data signal)))

(defmacro defsignal [signal-name Signal]
  `(def ~signal-name
		(Melement. 50 (float (get-note-freq ~signal-name)) (float 1) play-signal (new ~Signal 440 1 (.sampleRate (.getLineOut @*minim*))))))

(defsignal mysin SquareWave)
(defsignal mysine SineWave)

(defn setFreq [signal freq]
  (assoc signal :pitch freq))

;;(p (pattern [kick A5 Bb5]))
;;(sig sin [A B C D])

(defmacro get-note-freq
  "Converts Ab4 to notes: A b 4 and return frequency"
  [snote]
  `(let [note# (map char (str '~snote))]
	 (calc-freq
	  (if (> (count note#) 2)
		(zipmap
		 '(:note :half :scale )
		 note#)
		(zipmap
		 '(:note :scale)
		 note#)))))

(defn calc-freq [sig-note]
  (*
   (clojure.core/+ 
	(cond (= \C (:note sig-note)) 16.35
		  (= \D (:note sig-note)) 18.35
		  (= \E (:note sig-note)) 20.60
		  (= \F (:note sig-note)) 21.83
		  (= \G (:note sig-note)) 24.50
		  (= \A (:note sig-note)) 27.50
		  (= \B (:note sig-note)) 30.87)
	(cond (not (contains? sig-note :half)) 0
		  (= \# (:half sig-note)) 1.0594
		  (= \b (:half sig-note)) -1.0594))
   (Math/pow 2 (Integer/parseInt (str (:scale sig-note))))))
	  


(defn square []
  (let [out (.getLineOut @*minim*)
		sine (.sampleRate out)]
	(.addSignal out sine)
  (.setFreq @*sine* 40)
  
  )



  float pan = map(mouseX, 0, width, -1, 1);
  sine.setPan(pan);


(.addSignal @*outp* @*sine*)
(.removeSignal @*outp* @*sine*)
(.setFreq @*sine* 410)

(doseq [i (take 400 (repeatedly (fn [] (rand 400))))]
  (.setFreq @*sine* i)
  (Thread/sleep 100))


(.removeSignal @*outp* @*sine*)
 
(.clearSignals @*outp*)

(.playNote @*outp* (float 0) (float 1.0) "A"))

(.close @*outp*)


(defn print-progress-bar [percent]
  (let [bar (StringBuilder. "[")] 
    (doseq [i (range 50)]
      (cond (< i (int (/ percent 2))) (.append bar "=")
            (= i (int (/ percent 2))) (.append bar ">")
            :else (.append bar " ")))
    (.append bar (str "] " percent "%     "))
    (print "\r" (.toString bar))
    (flush)))


