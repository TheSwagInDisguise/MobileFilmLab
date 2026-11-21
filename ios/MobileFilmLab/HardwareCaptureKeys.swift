import SwiftUI
import AVKit

/// Public hardware capture events; no hidden volume control, audio session or microphone access.
struct HardwareCaptureKeys:UIViewRepresentable {
    var enabled:Bool
    var action:()->Void
    func makeCoordinator()->Coordinator {Coordinator(action:action)}
    func makeUIView(context:Context)->UIView {
        let view=UIView();let coordinator=context.coordinator
        let interaction=AVCaptureEventInteraction {event in if event.phase == .ended {coordinator.action()}}
        interaction.isEnabled=enabled;view.addInteraction(interaction);context.coordinator.interaction=interaction
        return view
    }
    func updateUIView(_ uiView:UIView,context:Context) {context.coordinator.action=action;context.coordinator.interaction?.isEnabled=enabled}
    final class Coordinator {var action:()->Void;var interaction:AVCaptureEventInteraction?;init(action:@escaping ()->Void) {self.action=action}}
}
