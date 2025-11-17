import MapKit
import SwiftUI

class SwiftUIHistoryClusterAnnotationView: MKAnnotationView {
    
    private var hostingController: UIHostingController<HistoryClusterView>?
    private var currentCount: Int = 0
    private var isCurrentlySelected: Bool = false
    
    override init(annotation: MKAnnotation?, reuseIdentifier: String?) {
        super.init(annotation: annotation, reuseIdentifier: reuseIdentifier)
        setupView()
    }
    
    required init?(coder aDecoder: NSCoder) {
        super.init(coder: aDecoder)
        setupView()
    }
    
    private func setupView() {
        backgroundColor = .clear
        canShowCallout = false
        
        // Set initial frame
        frame = CGRect(x: 0, y: 0, width: 60, height: 30)
        centerOffset = CGPoint(x: 0, y: -frame.height / 2)
        
        setupSwiftUIView()
    }
    
    private func setupSwiftUIView() {
        // Remove existing hosting controller if any
        hostingController?.view.removeFromSuperview()
        hostingController = nil
        
        // Create SwiftUI view
        let swiftUIView = HistoryClusterView(count: currentCount, isSelected: isCurrentlySelected)
        hostingController = UIHostingController(rootView: swiftUIView)
        
        guard let hostingController = hostingController else { return }
        
        // Configure hosting controller
        hostingController.view.backgroundColor = .clear
        hostingController.view.translatesAutoresizingMaskIntoConstraints = false
        
        // Add to annotation view
        addSubview(hostingController.view)
        
        // Setup constraints
        NSLayoutConstraint.activate([
            hostingController.view.centerXAnchor.constraint(equalTo: centerXAnchor),
            hostingController.view.centerYAnchor.constraint(equalTo: centerYAnchor),
            hostingController.view.widthAnchor.constraint(lessThanOrEqualToConstant: 80),
            hostingController.view.heightAnchor.constraint(equalToConstant: 30)
        ])
    }
    
    func updateCluster(count: Int, isSelected: Bool = false) {
        guard currentCount != count || isCurrentlySelected != isSelected else { return }
        
        currentCount = count
        isCurrentlySelected = isSelected
        
        // Update SwiftUI view
        let swiftUIView = HistoryClusterView(count: count, isSelected: isSelected)
        hostingController?.rootView = swiftUIView
    }
    
    override func setSelected(_ selected: Bool, animated: Bool) {
        super.setSelected(selected, animated: animated)
        updateCluster(count: currentCount, isSelected: selected)
    }
    
    override func prepareForReuse() {
        super.prepareForReuse()
        currentCount = 0
        isCurrentlySelected = false
        hostingController?.rootView = HistoryClusterView(count: 0, isSelected: false)
    }
} 