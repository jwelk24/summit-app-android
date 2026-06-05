//
//  Item.swift
//  Summit
//
//  Created by Jayson Welker on 6/5/26.
//

import Foundation
import SwiftData

@Model
final class Item {
    var timestamp: Date
    
    init(timestamp: Date) {
        self.timestamp = timestamp
    }
}
