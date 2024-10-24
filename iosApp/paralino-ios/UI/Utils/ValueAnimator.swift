//
//  ValueAnimator.swift
//  paralino-ios
//
//  Created by Zelimir on 2024-10-22.
//

import UIKit

class ValueAnimator {
    // MARK: - Types
    typealias AnimationUpdateHandler = (CGFloat) -> Void
    typealias AnimationCompletionHandler = () -> Void
    
    enum TimingFunction {
        case linear
        case easeInOut
        case easeIn
        case easeOut
        case spring(damping: CGFloat, velocity: CGFloat)
        case custom((CGFloat) -> CGFloat)
        
        func apply(_ t: CGFloat) -> CGFloat {
            switch self {
            case .linear:
                return t
            case .easeInOut:
                return t < 0.5 ? 4 * t * t * t : 1 - pow(-2 * t + 2, 3) / 2
            case .easeIn:
                return t * t * t
            case .easeOut:
                return 1 - pow(1 - t, 3)
            case .spring(let damping, let velocity):
                return springFunction(t, damping: damping, velocity: velocity)
            case .custom(let function):
                return function(t)
            }
        }
        
        private func springFunction(_ t: CGFloat, damping: CGFloat, velocity: CGFloat) -> CGFloat {
            let b = damping
            let v = velocity
            return 1 - exp(-b * t) * cos(v * t)
        }
    }
    
    // MARK: - Properties
    private var displayLink: CADisplayLink?
    private var startTime: CFTimeInterval = 0
    private var duration: TimeInterval
    private var fromValue: CGFloat
    private var toValue: CGFloat
    private var currentValue: CGFloat
    private var timingFunction: TimingFunction
    private var updateHandler: AnimationUpdateHandler?
    private var completionHandler: AnimationCompletionHandler?
    
    // MARK: - Initialization
    init(
        duration: TimeInterval = 0.3,
        timingFunction: TimingFunction = .easeInOut
    ) {
        self.duration = duration
        self.timingFunction = timingFunction
        self.fromValue = 0
        self.toValue = 0
        self.currentValue = 0
    }
    
    // MARK: - Public Methods
    func animate(
        from: CGFloat,
        to: CGFloat,
        duration: TimeInterval? = nil,
        timingFunction: TimingFunction? = nil,
        onUpdate: @escaping AnimationUpdateHandler,
        onComplete: AnimationCompletionHandler? = nil
    ) {
        // Stop any ongoing animation
        stop()
        
        // Update properties
        self.fromValue = from
        self.toValue = to
        self.currentValue = from
        self.updateHandler = onUpdate
        self.completionHandler = onComplete
        
        if let duration = duration {
            self.duration = duration
        }
        
        if let timingFunction = timingFunction {
            self.timingFunction = timingFunction
        }
        
        // Start animation
        startTime = CACurrentMediaTime()
        displayLink = CADisplayLink(target: self, selector: #selector(handleUpdate))
        displayLink?.preferredFramesPerSecond = 60
        displayLink?.add(to: .main, forMode: .common)
    }
    
    func stop() {
        displayLink?.invalidate()
        displayLink = nil
    }
    
    // MARK: - Private Methods
    @objc private func handleUpdate(_ displayLink: CADisplayLink) {
        let elapsed = CACurrentMediaTime() - startTime
        let progress = CGFloat(min(elapsed / duration, 1.0))
        
        // Apply timing function
        let timing = timingFunction.apply(progress)
        
        // Calculate current value
        currentValue = fromValue + (toValue - fromValue) * timing
        
        // Call update handler
        updateHandler?(currentValue)
        
        // Check for completion
        if progress >= 1.0 {
            stop()
            completionHandler?()
        }
    }
}
