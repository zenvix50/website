import mongoose from 'mongoose';

const assetSchema = new mongoose.Schema({
  os: String,
  label: String,
  format: String,
  url: String,
  size: Number,
  checksum: String,
  arch: String,
});

const releaseSchema = new mongoose.Schema({
  version: { type: String, required: true, unique: true },
  tagName: String,
  releasedAt: Date,
  changelog: String,
  assets: [assetSchema],
  isLatest: { type: Boolean, default: false },
  downloadCount: { type: Number, default: 0 },
  githubUrl: String,
}, { timestamps: true });

export default mongoose.model('Release', releaseSchema);
