/*
Copyright (C) 2015 Apple Inc. All Rights Reserved.
See the Apple Developer Program License Agreement for this file's licensing information.
All use of these materials is subject to the terms of the Apple Developer Program License Agreement.

Abstract:
PlayerView.swift: Simple subclass of UIView containing an AVPlayerLayer layer to which the output of an AVPlayer object can be directed.
*/

import UIKit
import AVFoundation

class PlayerView: UIView {
	// MARK: Properties
	
	var player: AVPlayer? {
		get {
			return playerLayer.player
		}
		
		set {
			playerLayer.player = newValue
		}
	}
	
	var playerLayer: AVPlayerLayer {
		return layer as! AVPlayerLayer
	}
	
    // MARK: Functions
	override class var layerClass : AnyClass {
		return AVPlayerLayer.self
	}
}
