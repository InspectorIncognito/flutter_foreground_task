//
//  BusableController.swift
//  flutter_foreground_task
//
//  Created by Agustin Antoine on 08-06-23.
//

class BusableController: NSObject {
    typealias Subs = KeyValuePairs<Bus.Events, (Notification) -> Void>
    // Stub, should be overriden
    var SubscriptionEvents: Subs { get { return [:] } }
    
    private var Desubsrcibers: [Bus.Unsubscriber?] = []
}

// MARK: - Bus Sub/De-sub
extension BusableController {
    func register() {
        print("BusableController: registering bus events")
        for sub in self.SubscriptionEvents {
            print(sub.key)
            self.Desubsrcibers.append(Bus.shared.on(event: sub.key, cb: sub.value))
        }
    }
    
    func deregister() {
        print("BusableController: removing bus events")
        let cnt = self.Desubsrcibers.count
        for i in 0 ..< cnt {
            var m = self.Desubsrcibers[i]
            m?()
            self.Desubsrcibers[i] = nil
            m = nil
        }
    }
}
