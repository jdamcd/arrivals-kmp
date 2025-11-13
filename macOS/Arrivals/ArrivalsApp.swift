@preconcurrency import ArrivalsLib
import SwiftUI

@main
struct ArrivalsApp: App {
    @NSApplicationDelegateAdaptor(AppDelegate.self) var delegate
    @State private var settingsWindow: NSWindow?

    init() {
        ArrivalsKt.doInitKoin()
    }

    var body: some Scene {
        Settings {
            SettingsView()
                .background(WindowAccessor(window: $settingsWindow))
                .onReceive(NotificationCenter.default.publisher(for: NSWindow.willCloseNotification)) { notif in
                    if let window = notif.object as? NSWindow {
                        let settingsNum = settingsWindow?.windowNumber ?? -1
                        print("Window \(window.windowNumber) is closing. Settings is \(settingsNum).")
                        if window.windowNumber == settingsWindow?.windowNumber {
                            NSApplication.accessoryMode()
                        }
                    }
                }
        }
    }
}

struct WindowAccessor: NSViewRepresentable {
    @Binding var window: NSWindow?

    func makeNSView(context _: Context) -> NSView {
        let view = NSView()
        DispatchQueue.main.async { [weak view] in
            window = view?.window
        }
        return view
    }

    func updateNSView(_: NSView, context _: Context) {}
}

class PopoverState: ObservableObject {
    @Published var isShown = false
}

@MainActor
class AppDelegate: NSObject, NSApplicationDelegate {
    private var statusItem: NSStatusItem?
    private let popover = NSPopover()
    private let popoverDelegate = PopoverDelegate()

    @ObservedObject var popoverState = PopoverState()

    func applicationDidFinishLaunching(_: Notification) {
        let menuView = ArrivalsView(
            popoverState: popoverState,
            onOpenSettings: { NSApplication.foregroundMode() },
            onQuit: { NSApplication.quit() }
        )

        popover.behavior = .transient
        popover.animates = true
        popover.contentViewController = NSViewController()
        popover.contentViewController?.view = NSHostingView(rootView: menuView)

        popover.delegate = popoverDelegate
        popoverDelegate.onShow = {
            self.popoverState.isShown = true
        }
        popoverDelegate.onClose = {
            self.popoverState.isShown = false
        }

        statusItem = NSStatusBar.system.statusItem(withLength: NSStatusItem.variableLength)
        if let menuButton = statusItem?.button {
            menuButton.image = NSImage(systemSymbolName: "tram.fill", accessibilityDescription: nil)
            menuButton.action = #selector(menuButtonToggle)
        }
    }

    @objc func menuButtonToggle(sender: AnyObject) {
        if popover.isShown {
            popover.performClose(sender)
        } else {
            if let menuButton = statusItem?.button {
                popover.show(relativeTo: menuButton.bounds, of: menuButton, preferredEdge: NSRectEdge.minY)
                popover.contentViewController?.view.window?.makeKey()
            }
        }
    }
}

class PopoverDelegate: NSObject, NSPopoverDelegate {
    var onShow: (() -> Void)?

    func popoverWillShow(_: Notification) {
        onShow?()
    }

    var onClose: (() -> Void)?

    func popoverDidClose(_: Notification) {
        onClose?()
    }
}

extension NSApplication {
    static func foregroundMode() {
        NSApp.setActivationPolicy(.regular)
        NSApp.activate(ignoringOtherApps: true)
    }

    static func accessoryMode() {
        NSApp.setActivationPolicy(.accessory)
    }

    static func quit() {
        NSApp.terminate(self)
    }
}
