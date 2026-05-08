import mongoose from 'mongoose';

const downloadSchema = new mongoose.Schema({
  version: String,
  os: String,
  format: String,
  ip: String,
  userAgent: String,
  timestamp: { type: Date, default: Date.now },
});

export default mongoose.model('Download', downloadSchema);
